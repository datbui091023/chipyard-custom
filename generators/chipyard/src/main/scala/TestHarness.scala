package chipyard

import chisel3._
import scala.collection.mutable.{ArrayBuffer}
import freechips.rocketchip.diplomacy.{LazyModule}
import freechips.rocketchip.config.{Field, Parameters}

import chipyard.harness.{ApplyHarnessBinders, HarnessBinders}

// -------------------------------
// Chipyard Test Harness
// -------------------------------

case object BuildTop extends Field[Parameters => LazyModule]((p: Parameters) => new ChipTop()(p))

trait HasTestHarnessFunctions {
  val lazySystem: LazyModule
  val harnessFunctions = ArrayBuffer.empty[HasHarnessSignalReferences => Seq[Any]]
  val portMap = scala.collection.mutable.Map[String, Seq[Data]]()
}

trait HasHarnessSignalReferences {
  def harnessClock: Clock
  def harnessReset: Reset
  def dutReset: Reset
  def success: Bool
}

class TestHarness(implicit val p: Parameters) extends Module with HasHarnessSignalReferences {
  val io = IO(new Bundle {
    val success = Output(Bool())
  })

  val lazyDut = LazyModule(p(BuildTop)(p)).suggestName("chiptop")
  val dut = Module(lazyDut.module)
  io.success := false.B

  val harnessClock = clock
  val harnessReset = WireInit(reset)
  val success = io.success

  val dutReset = reset.asAsyncReset

  lazyDut match { case d: HasTestHarnessFunctions =>
    d.harnessFunctions.foreach(_(this))
    ApplyHarnessBinders(this, d.lazySystem, p(HarnessBinders), d.portMap.toMap)
  }
}

