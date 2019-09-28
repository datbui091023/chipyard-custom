Tops, Test-Harnesses, and the Test-Driver
====================================

The three highest levels of hierarchy in a Chipyard
SoC are the Top (DUT), ``TestHarness``, and the ``TestDriver``.
The Top and ``TestHarness`` are both emitted by Chisel generators.
The ``TestDriver`` serves as our testbench, and is a verilog
file in Rocket Chip.


Top/DUT
-------------------------

The top-level module of a Rocket Chip SoC is composed via cake-pattern.
Specifically, "Tops" extend a ``System``, which extends a ``Subsystem``, which extends a ``BaseSubsystem``.


BaseSubsystem
^^^^^^^^^^^^^^^^^^^^^^^^^

The ``BaseSubsystem`` is defined in ``generators/rocketchip/src/main/scala/subsystem/BaseSubsystem.scala``.
Looking at the ``BaseSubsystem`` abstract class, we see that this class instantiates the top-level buses
(frontbus, systembus, peripherybus, etc.), but does not specify a topology.
We also see this class define several ``ElaborationArtefacts``, files emitted after Chisel elaboration
(e.g. the device tree string, and the diplomacy graph visualization GraphML file).

Subsystem
^^^^^^^^^^^^^^^^^^^^^^^^^

Looking in `generators/utilities/src/main/scala/Subsystem.scala <https://github.com/ucb-bar/chipyard/blob/master/generators/utilities/src/main/scala/Subsystem.scala>`__, we can see how Chipyard's ``Subsystem``
extends the ``BaseSubsystem`` abstract class. ``Subsystem`` mixes in the ``HasBoomAndRocketTiles`` trait that
defines and instantiates BOOM or Rocket tiles, depending on the parameters specified.
We also connect some basic IOs for each tile here, specifically the hartids and the reset vector.

System
^^^^^^^^^^^^^^^^^^^^^^^^^

``generators/utilities/src/main/scala/System.scala`` completes the definition of the ``System``.

- ``HasHierarchicalBusTopology`` is defined in Rocket Chip, and specifies connections between the top-level buses
- ``HasAsyncExtInterrupts`` and ``HasExtInterruptsModuleImp`` adds IOs for external interrupts and wires them appropriately to tiles
- ``CanHave...AXI4Port`` adds various Master and Slave AXI4 ports, adds TL-to-AXI4 converters, and connects them to the appropriate buses
- ``HasPeripheryBootROM`` adds a BootROM device

Tops
^^^^^^^^^^^^^^^^^^^^^^^^^

A SoC Top then extends the ``System`` class with any config-specific components. There are two "classes" of Tops in Chipyard that enable different bringup methods.
Please refer to :ref:`Communicating with the DUT` for more information on these bringup methods.

- ``Top`` is the default setup. These top modules instantiate a serial module which interfaces with the ``TestHarness``. In addition, the Debug Transfer Module (DTM) is removed and replaced with a TSI-based bringup flow. All other example "Tops" (except the ``TopWithDTM``) extend this Top as the "base" top-level system.
- ``TopWithDTM`` does not include the TSI-based bringup flow. Instead it keeps the Debug Transfer Module (DTM) within the design so that you can use a DMI-based or JTAG-based bringup.

For a custom Top a mixed-in trait adds the additional modules or IOs (an example of this is ``TopWithGPIO``).

TestHarness
-------------------------

There are two variants of ``TestHarness`` generators in Chipyard, both located in
`generators/example/src/main/scala/TestHarness.scala <https://github.com/ucb-bar/chipyard/blob/master/generators/example/src/main/scala/TestHarness.scala>`__.
One is designed for TSI-based bringup, while the other performs DTM-based bringup.
See :ref:`Communicating with the DUT` for more information on these two methodologies.

The wiring between the ``TestHarness`` and the Top are performed in methods defined in mixins added to the Top.
When these methods are called from the ``TestHarness``, they may instantiate modules within the scope of the harness,
and then connect them to the DUT. For example, the ``connectSimAXIMem`` method defined in the
``CanHaveMasterAXI4MemPortModuleImp`` trait, when called from the ``TestHarness``, will instantiate ``SimAXIMem``s
and connect them to the correct IOs of the top.

While this roundabout way of attaching to the IOs of the top may seem to be unnecessarily complex, it allows the designer to compose
custom traits together without having to worry about the details of the implementation of any particular trait.

Specifying a Top
^^^^^^^^^^^^^^^^^^^^^^^^^

To see why the Top connection method is useful, consider the case where we want to use a custom Top with additional GPIO pins.
In `generators/example/src/main/scala/Top.scala <https://github.com/ucb-bar/chipyard/blob/master/generators/example/src/main/scala/Top.scala>`__,
we can see how the ``TopWithGPIO`` class adds the ``HasPeripheryGPIO`` trait. This trait adds IOs to the Top module,
instantiates a TileLink GPIO node, and connects it to the proper buses.

If we look at the ``WithGPIOTop`` mixin in the ``ConfigMixins.scala`` file, we see that adding this mixin to the top-level Config overrides the
``BuildTop`` key with a custom function that both instantiates the custom Top, and drives all the GPIO pins.
When the ``TestHarness`` looks up the ``BuildTop`` key, this function will run and perform the specified wiring, and then return the Top module.

TestDriver
-------------------------

The ``TestDriver`` is defined in ``generators/rocketchip/src/main/resources/vsrc/TestDriver.v``.
This verilog file executes a simulation by instantiating the ``TestHarness``, driving the clock and reset signals, and interpreting the success output.
This file is compiled with the generated verilog for the ``TestHarness`` and the Top to produce a simulator.
