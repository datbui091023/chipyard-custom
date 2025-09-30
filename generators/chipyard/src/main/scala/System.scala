//******************************************************************************
// Copyright (c) 2019 - 2019, The Regents of the University of California (Regents).
// All Rights Reserved. See LICENSE and LICENSE.SiFive for license details.
//------------------------------------------------------------------------------

package chipyard

import chisel3._

import org.chipsalliance.cde.config.{Parameters, Field}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.devices.tilelink._
// import org.chipsalliance.diplomacy.bundlebridge._
import freechips.rocketchip.diplomacy._
import org.chipsalliance.diplomacy.nodes._
import freechips.rocketchip.prci._
import freechips.rocketchip.util.{DontTouch}
import chipyard.iobinders._

// ---------------------------------------------------------------------
// Base system that uses the debug test module (dtm) to bringup the core
// ---------------------------------------------------------------------

/**
 * Base top with periphery devices and ports, and a BOOM + Rocket subsystem
 */

 // datbt11
 // In case of no in-chip BootROM, Reset Vector addr still need to be driven to every tiles
 trait CanHaveExternalResetVector { this: ChipyardSubsystem =>
  implicit val p: Parameters
  p(ExternalResetVecKey).foreach { addr =>
    ExternalResetVec.attach(addr, this)
  }
}
// datbt11

class ChipyardSystem(implicit p: Parameters) extends ChipyardSubsystem
  with HasAsyncExtInterrupts
  with CanHaveMasterTLMemPort // export TL port for outer memory
  with CanHaveMasterAXI4MemPort // expose AXI port for outer mem
  with CanHaveMasterAXI4MMIOPort
  with CanHaveSlaveAXI4Port
  // //
  // with CanHaveExternalResetVector   // datbt11
  // with CanHaveTLPunchThrough        //datbt11
  // with CanTapCBusClkRst             // datbt11
  // //
{

  val bootROM  = p(BootROMLocated(location)).map { BootROM.attach(_, this, CBUS) }
  val maskROMs = p(MaskROMLocated(location)).map { MaskROM.attach(_, this, CBUS) }

  override lazy val module = new ChipyardSystemModule(this)
}

/**
 * Base top module implementation with periphery devices and ports, and a BOOM + Rocket subsystem
 */
class ChipyardSystemModule(_outer: ChipyardSystem) extends ChipyardSubsystemModuleImp(_outer)
  with HasRTCModuleImp
  with HasExtInterruptsModuleImp
  with DontTouch

// ------------------------------------
// TL Mem Port Mixin
// ------------------------------------

// Similar to ExtMem but instantiates a TL mem port
case object ExtTLMem extends Field[Option[MemoryPortParams]](None)

/** Adds a port to the system intended to master an TL DRAM controller. */
trait CanHaveMasterTLMemPort { this: BaseSubsystem =>

  require(!(p(ExtTLMem).nonEmpty && p(ExtMem).nonEmpty),
    "Can only have 1 backing memory port. Use ExtTLMem for a TL memory port or ExtMem for an AXI memory port.")

  private val memPortParamsOpt = p(ExtTLMem)
  private val portName = "tl_mem"
  private val device = new MemoryDevice
  private val idBits = memPortParamsOpt.map(_.master.idBits).getOrElse(1)
  private val mbus = tlBusWrapperLocationMap.lift(MBUS).getOrElse(locateTLBusWrapper(SBUS))

  val memTLNode = TLManagerNode(memPortParamsOpt.map({ case MemoryPortParams(memPortParams, nMemoryChannels, _) =>
    Seq.tabulate(nMemoryChannels) { channel =>
      val base = AddressSet.misaligned(memPortParams.base, memPortParams.size)
      val filter = AddressSet(channel * mbus.blockBytes, ~((nMemoryChannels-1) * mbus.blockBytes))

     TLSlavePortParameters.v1(
       managers = Seq(TLSlaveParameters.v1(
         address            = base.flatMap(_.intersect(filter)),
         resources          = device.reg,
         regionType         = RegionType.UNCACHED, // cacheable
         executable         = true,
         supportsGet        = TransferSizes(1, mbus.blockBytes),
         supportsPutFull    = TransferSizes(1, mbus.blockBytes),
         supportsPutPartial = TransferSizes(1, mbus.blockBytes))),
         beatBytes = memPortParams.beatBytes)

    }
  }).toList.flatten)

  // disable inwards monitors from node since the class with this trait (i.e. DigitalTop)
  // doesn't provide an implicit clock to those monitors
  mbus.coupleTo(s"memory_controller_port_named_$portName") {
    (DisableMonitors { implicit p => memTLNode :*= TLBuffer() }
      :*= TLSourceShrinker(1 << idBits)
      :*= TLWidthWidget(mbus.beatBytes)
      :*= _)
  }

  val mem_tl = InModuleBody { memTLNode.makeIOs() }
}

//   V1   // DATBT11
// trait CanHaveTLPunchThrough{ this: BaseSubsystem =>
//     // p(TLPunchKey).foreach { prm =>
//     private val cbus = tlBusWrapperLocationMap.lift(CBUS).getOrElse(locateTLBusWrapper(SBUS))   // select CBUS
//     private val beat_tmp: Int = 8
//     private val base: BigInt = 0x10000
//     private val size: Int = 0x10000
//     private val beat = if (beat_tmp > 0) beat_tmp else cbus.beatBytes
//     private val resources: Seq[Resource] = new SimpleDevice("extrom", Seq("vendor,extrom0")).reg("mem")
//     private val name = "tl_rom"
//     private val bootROMDomainWrapper = cbus.generateSynchronousDomain(name).suggestName(s"${name}_domain")
//     val TLManagerParams: TLSlaveParameters = 
//     TLSlaveParameters.v1(
//       address     = List(AddressSet(base, size-1)),
//       resources   = resources,
//       regionType  = RegionType.UNCACHED,
//       executable  = true,
//       supportsGet = TransferSizes(1, beat),
//       fifoId      = Some(0)
//       )
//     val TLManagerPortParams: TLSlavePortParameters =
//       TLSlavePortParameters.v1(
//       managers = Seq(TLManagerParams),
//       beatBytes = beat
//     )

//     val tl_rom = bootROMDomainWrapper{TLToBundleBridge(TLManagerPortParams)}
//     tl_rom := cbus.coupleTo("ext_bootrom"){ TLFragmenter(cbus) := _ }
//     val romSink = BundleBridgeSink[TLBundle]()
//     romSink := tl_rom
//     val tl_rom_io = InModuleBody { romSink.makeIOs().suggestName("tl_rom") }
// }
// V2 // datbt11
trait CanHaveTLPunchThrough{ this: BaseSubsystem =>
    // p(TLPunchKey).foreach { prm =>
    private val cbus = tlBusWrapperLocationMap.lift(CBUS).getOrElse(locateTLBusWrapper(SBUS))   // select CBUS
    private val beat_tmp: Int = 8
    private val base: BigInt = 0x10000
    private val size: Int = 0x10000
    private val beat = if (beat_tmp > 0) beat_tmp else cbus.beatBytes
    private val resources: Seq[Resource] = new SimpleDevice("extrom", Seq("vendor,extrom0")).reg("mem")
    private val name = "tl_rom"
    private val bootROMDomainWrapper = cbus.generateSynchronousDomain(name).suggestName(s"${name}_domain")
    val TLManagerParams: TLSlaveParameters = 
    TLSlaveParameters.v1(
      address     = List(AddressSet(base, size-1)),
      resources   = resources,
      regionType  = RegionType.UNCACHED,
      executable  = true,
      supportsGet = TransferSizes(1, beat),
      supportsPutFull =    TransferSizes.none,
      supportsPutPartial = TransferSizes.none,
      fifoId      = Some(0)
      )
    val TLManagerPortParams: TLSlavePortParameters =
      TLSlavePortParameters.v1(
      managers = Seq(TLManagerParams),
      beatBytes = beat
    )

    val tl_rom = bootROMDomainWrapper{LazyModule(new TLToBundleBridge(TLManagerPortParams))}
    tl_rom.node := cbus.coupleTo("ext_bootrom"){  TLFragmenter(cbus) := _ }
    val romSink = BundleBridgeSink[TLBundle]()
    romSink := tl_rom.node
    val tl_rom_io = InModuleBody { romSink.makeIOs().suggestName("tl_rom") }
}

trait CanTapCBusClkRst { this: BaseSubsystem =>
  private val cbus = tlBusWrapperLocationMap.lift(CBUS).getOrElse(locateTLBusWrapper(SBUS))   // select CBUS
  private val cbusClkSink = ClockSinkNode(Seq(ClockSinkParameters(name=Some("cbus_clock_tap"))))
  cbusClkSink := cbus.fixedClockNode
 // clock
  val cbus_clk_out: org.chipsalliance.diplomacy.lazymodule.ModuleValue[Clock] = InModuleBody {
    val clk = IO(Output(Clock())).suggestName("cbus_clk")
    val (bundle, _) = cbusClkSink.in.head
    clk := bundle.clock
    clk
  }
  // reset
  val cbus_rst_out: org.chipsalliance.diplomacy.lazymodule.ModuleValue[AsyncReset] = InModuleBody {
    val rst = IO(Output(AsyncReset())).suggestName("cbus_reset")
    val (bundle, _) = cbusClkSink.in.head
    rst := bundle.reset.asAsyncReset
    rst
  }
}
//datbt11









