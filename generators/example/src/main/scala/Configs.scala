package example

import chisel3._

import freechips.rocketchip.config.{Config}
import freechips.rocketchip.subsystem.{WithRoccExample, WithNMemoryChannels, WithNBigCores, WithRV32, WithExtMemSize, WithNBanks}

import testchipip._

// --------------
// Rocket Configs
// --------------

class BaseRocketConfig extends Config(
  new WithBootROM ++
  new freechips.rocketchip.system.DefaultConfig)

class DefaultRocketConfig extends Config(
  new WithNormalRocketTop ++
  new BaseRocketConfig)

class HwachaConfig extends Config(
  new hwacha.DefaultHwachaConfig ++
  new DefaultRocketConfig)

class RoccRocketConfig extends Config(
  new WithRoccExample ++
  new DefaultRocketConfig)

class PWMRocketConfig extends Config(
  new WithPWMRocketTop ++
  new BaseRocketConfig)

class PWMAXI4RocketConfig extends Config(
  new WithPWMAXI4RocketTop ++
  new BaseRocketConfig)

class SimBlockDeviceRocketConfig extends Config(
  new WithBlockDevice ++
  new WithSimBlockDeviceRocketTop ++
  new BaseRocketConfig)

class BlockDeviceModelRocketConfig extends Config(
  new WithBlockDevice ++
  new WithBlockDeviceModelRocketTop ++
  new BaseRocketConfig)

class GPIORocketConfig extends Config(
  new WithGPIO ++
  new WithGPIORocketTop ++
  new BaseRocketConfig)

class DualCoreRocketConfig extends Config(
  new WithNBigCores(2) ++
  new DefaultRocketConfig)

class RV32RocketConfig extends Config(
  new WithRV32 ++
  new DefaultRocketConfig)

class GB1MemoryConfig extends Config(
  new WithExtMemSize((1<<30) * 1L) ++
  new DefaultRocketConfig)

// ------------
// BOOM Configs
// ------------

class BaseBoomConfig extends Config(
  new WithBootROM ++
  new boom.system.BoomConfig)

class SmallBaseBoomConfig extends Config(
  new WithBootROM ++
  new boom.system.SmallBoomConfig)

class DefaultBoomConfig extends Config(
  new WithNormalBoomTop ++
  new BaseBoomConfig)

class SmallDefaultBoomConfig extends Config(
  new WithNormalBoomTop ++
  new SmallBaseBoomConfig)

class HwachaBoomConfig extends Config(
  new hwacha.DefaultHwachaConfig ++
  new DefaultBoomConfig)

class RoccBoomConfig extends Config(
  new WithRoccExample ++
  new DefaultBoomConfig)

class PWMBoomConfig extends Config(
  new WithPWMBoomTop ++
  new BaseBoomConfig)

class PWMAXI4BoomConfig extends Config(
  new WithPWMAXI4BoomTop ++
  new BaseBoomConfig)

class SimBlockDeviceBoomConfig extends Config(
  new WithBlockDevice ++
  new WithSimBlockDeviceBoomTop ++
  new BaseBoomConfig)

class BlockDeviceModelBoomConfig extends Config(
  new WithBlockDevice ++
  new WithBlockDeviceModelBoomTop ++
  new BaseBoomConfig)

class GPIOBoomConfig extends Config(
  new WithGPIO ++
  new WithGPIOBoomTop ++
  new BaseBoomConfig)

/**
 * Slightly different looking configs since we need to override
 * the `WithNBoomCores` with the DefaultBoomConfig params
 */
class DualCoreBoomConfig extends Config(
  new WithNormalBoomTop ++
  new WithBootROM ++
  new boom.common.WithRVC ++
  new boom.common.DefaultBoomConfig ++
  new boom.system.WithNBoomCores(2) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new freechips.rocketchip.system.BaseConfig)

class DualCoreSmallBoomConfig extends Config(
  new WithNormalBoomTop ++
  new WithBootROM ++
  new boom.common.WithRVC ++
  new boom.common.WithSmallBooms ++
  new boom.common.DefaultBoomConfig ++
  new boom.system.WithNBoomCores(2) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new freechips.rocketchip.system.BaseConfig)

class RV32UnifiedBoomConfig extends Config(
  new WithNormalBoomTop ++
  new WithBootROM ++
  new boom.system.SmallRV32UnifiedBoomConfig)

// ---------------------
// BOOM + Rocket Configs
// ---------------------

class BaseBoomAndRocketConfig extends Config(
  new WithBootROM ++
  new WithRenumberHarts ++
  new boom.common.WithRVC ++
  new boom.common.DefaultBoomConfig ++
  new boom.system.WithNBoomCores(1) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new freechips.rocketchip.system.BaseConfig)

class SmallBaseBoomAndRocketConfig extends Config(
  new WithBootROM ++
  new WithRenumberHarts ++
  new boom.common.WithRVC ++
  new boom.common.WithSmallBooms ++
  new boom.common.DefaultBoomConfig ++
  new boom.system.WithNBoomCores(1) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new freechips.rocketchip.system.BaseConfig)

class DefaultBoomAndRocketConfig extends Config(
  new WithNormalBoomAndRocketTop ++
  new BaseBoomAndRocketConfig)

class SmallDefaultBoomAndRocketConfig extends Config(
  new WithNormalBoomAndRocketTop ++
  new SmallBaseBoomAndRocketConfig)

class HwachaBoomAndRocketConfig extends Config(
  new hwacha.DefaultHwachaConfig ++
  new DefaultBoomAndRocketConfig)

class RoccBoomAndRocketConfig extends Config(
  new WithRoccExample ++
  new DefaultBoomAndRocketConfig)

class PWMBoomAndRocketConfig extends Config(
  new WithPWMBoomAndRocketTop ++
  new BaseBoomAndRocketConfig)

class PWMAXI4BoomAndRocketConfig extends Config(
  new WithPWMAXI4BoomAndRocketTop ++
  new BaseBoomAndRocketConfig)

class SimBlockDeviceBoomAndRocketConfig extends Config(
  new WithBlockDevice ++
  new WithSimBlockDeviceBoomAndRocketTop ++
  new BaseBoomAndRocketConfig)

class BlockDeviceModelBoomAndRocketConfig extends Config(
  new WithBlockDevice ++
  new WithBlockDeviceModelBoomAndRocketTop ++
  new BaseBoomAndRocketConfig)

class GPIOBoomAndRocketConfig extends Config(
  new WithGPIO ++
  new WithGPIOBoomAndRocketTop ++
  new BaseBoomAndRocketConfig)

class DualCoreBoomAndOneRocketConfig extends Config(
  new WithNormalBoomAndRocketTop ++
  new WithBootROM ++
  new WithRenumberHarts ++
  new boom.common.WithRVC ++
  new boom.common.DefaultBoomConfig ++
  new boom.system.WithNBoomCores(2) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new freechips.rocketchip.system.BaseConfig)

class RV32BoomAndRocketConfig extends Config(
  new WithNormalBoomAndRocketTop ++
  new WithBootROM ++
  new WithRenumberHarts ++
  new boom.common.WithBoomRV32 ++
  new boom.common.WithRVC ++
  new boom.common.DefaultBoomConfig ++
  new boom.system.WithNBoomCores(1) ++
  new freechips.rocketchip.subsystem.WithoutTLMonitors ++
  new WithRV32 ++
  new freechips.rocketchip.subsystem.WithNBigCores(1) ++
  new freechips.rocketchip.system.BaseConfig)
