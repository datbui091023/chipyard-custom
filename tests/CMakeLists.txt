########################################################################################################################
# file:  CMakeLists.txt
#
# usage: 
#   Edit "VARIABLES"-section to suit project requirements.
#   Build instructions:
#     cmake -S ./ -B ./build/ -D CMAKE_BUILD_TYPE=Debug
#     cmake --build ./build/ --target all
#   Cleaning:
#     cmake --build ./build/ --target clean
########################################################################################################################
cmake_minimum_required(VERSION 3.10)

# Thêm ASM để build crt0.S
project(chipyard-tests LANGUAGES C CXX ASM)
# ---- Địa chỉ UART/Finisher (CHỈNH THEO SOC CỦA BẠN hoặc truyền khi cmake -D...)
set(UART_BASE      "0x10013000UL" CACHE STRING "UART base")
set(FINISHER_BASE  "0x00100000UL" CACHE STRING "Finisher base")
set(FINISHER_STYLE "1"            CACHE STRING "0=sifive-test, 1=htif-like")

#################################
# RISCV Toolchain
#################################
set(CMAKE_SYSTEM_NAME      "Generic" CACHE STRING "")
set(CMAKE_SYSTEM_PROCESSOR "riscv"   CACHE STRING "")

set(TOOLCHAIN_PREFIX "riscv64-unknown-elf-")
set(CMAKE_AR         "${TOOLCHAIN_PREFIX}ar")
set(CMAKE_ASM_COMPILER "${TOOLCHAIN_PREFIX}gcc")
set(CMAKE_C_COMPILER   "${TOOLCHAIN_PREFIX}gcc")
set(CMAKE_CXX_COMPILER "${TOOLCHAIN_PREFIX}g++")
# set(CMAKE_LINKER    "${TOOLCHAIN_PREFIX}ld")

set(CMAKE_OBJCOPY    "${TOOLCHAIN_PREFIX}objcopy")
set(CMAKE_OBJDUMP    "${TOOLCHAIN_PREFIX}objdump")
set(CMAKE_SIZE       "${TOOLCHAIN_PREFIX}size")

# Đặt đuôi file thực thi
set(CMAKE_EXECUTABLE_SUFFIX ".riscv")

#################################
# Flags
#################################
# Kiến trúc: có CSR -> thêm zicsr. Bạn đang dùng FPU -> lp64d + imafd_zicsr
set(ARCH   "rv64imafdc_zicsr")
set(ABI    "lp64d")
set(CMODEL "medany")
set(ARCH_FLAGS -march=${ARCH} -mabi=${ABI} -mcmodel=${CMODEL})

# ❌ Không dùng specs HTIF; nếu muốn có nosys path, ta link tay -lnosys (xem dưới)
# set(SPECS "nosys.specs")
# set(SPEC_FLAGS -specs=${SPECS})

# Linker script KHÔNG HTIF
set(LINKER_SCRIPT "${CMAKE_SOURCE_DIR}/htif.ld")

# Compile flags
add_compile_options(
  -std=gnu99
  -O2 -Wall -Wextra
  -fno-common
  -ffreestanding -fno-builtin   # chạy bare/newlib, tránh builtin
  -ffunction-sections -fdata-sections
  ${ARCH_FLAGS}
)

# Link flags chung
add_link_options(
  -static
  -nostartfiles                 # dùng crt0.S của bạn, không kéo crt0.o toolchain
  -Wl,-e,_start                 # entry là _start trong crt0.S
  -Wl,--gc-sections
  -T ${LINKER_SCRIPT}           # ép dùng linker script DRAM (0x8000_0000)
  -Wl,--defsym=end=_end         # cho _sbrk của nosys (heap base)
  # -Wl,--no-warn-rwx-segments  # nếu muốn tắt cảnh báo RWX
)

#################################
# Targets
#################################
# hello: dùng crt0.S + hello.c
add_executable(hello crt0.S init.S syscalls_uart.c crtmain.S main.c hello.c traps.c tls.c)

# Link libs theo đúng thứ tự nhóm (không cần specs):
#   newlib nano + nosys (syscalls stub) + libgcc
target_link_options(hello PRIVATE
  -Wl,--start-group -lc_nano -lnosys -Wl,--end-group -lgcc
)

# Map file per-target
target_link_options(hello PRIVATE
  -Wl,-Map,$<TARGET_FILE_DIR:hello>/$<TARGET_FILE_BASE_NAME:hello>.map
)

# Re-link khi linker script thay đổi
set_property(TARGET hello PROPERTY LINK_DEPENDS ${LINKER_SCRIPT})
# ĐỊNH NGHĨA ĐỊA CHỈ MMIO 
target_compile_definitions(hello PRIVATE
  UART_BASE=0x10013000UL           # UART MMIO base (ví dụ SiFive UART)
  FINISHER_BASE=0x00100000UL       # Finisher MMIO base (đổi theo harness)
  FINISHER_STYLE=1                 # 1 = htif-like ((code<<1)|1), 0 = kiểu sifive-test
)
#################################
# Disassembly
#################################
add_custom_target(dump ALL)
function(add_dump_target target_name)
  add_custom_target(${target_name}-dump
    BYPRODUCTS ${CMAKE_BINARY_DIR}/${target_name}.dump
    COMMAND ${CMAKE_OBJDUMP} -D $<TARGET_FILE:${target_name}>
            > ${CMAKE_BINARY_DIR}/${target_name}.dump
    DEPENDS ${target_name}
    COMMENT "Generating disassembly for ${target_name}"
  )
  add_dependencies(${target_name}-dump ${target_name})
  add_dependencies(dump ${target_name}-dump)
endfunction()

add_dump_target(hello)
