// // generators/chipyard/src/main/scala/WithInlineSimDRAM.scala
// package chipyard                            // giữ nguyên nếu bạn đã để trong package này

// import org.chipsalliance.cde.config.{Config, Parameters}   // ← đổi import
// import chisel3.util.experimental.loadMemoryFromFileInline
// import testchipip.dram.SimDRAM

// /** Mix-in để nạp sẵn một file HEX vào SimDRAM trong TestHarness */
// class WithInlineSimDRAM(hex: String = "tests/build/hello.hex")
//   extends Config((site, here, up) => {                      // vẫn đúng cú pháp
//     case chipyard.harness.HarnessBinders =>
//       up(chipyard.harness.HarnessBinders) ++ Seq(
//         { (th: chipyard.harness.TestHarness, dut: Any) =>
//           implicit val p: Parameters = th.p
//           th.memZip.foreach {
//             case mod if mod.module.isInstanceOf[SimDRAM#SimDRAMImp] =>
//               val sdram = mod.module.asInstanceOf[SimDRAM#SimDRAMImp]
//               loadMemoryFromFileInline(sdram.ram, hex)
//           }
//         }
//       )
//   })
