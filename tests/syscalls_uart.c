// syscalls_uart.c — bare-metal syscalls via UART MMIO + Finisher MMIO
// Build flags (ví dụ):
//   -DUART_BASE=0x10013000UL -DFINISHER_BASE=0x100000UL
// hoặc chỉnh lại cho đúng map của bạn.

#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <stdint.h>
#include <unistd.h>

// ===== UART register map: SiFive-style =====
#ifndef UART_BASE
#  define UART_BASE 0x10013000UL    
#endif

#ifndef FINISHER_BASE
#  define FINISHER_BASE 0x00100000UL 
#endif

#ifndef FINISHER_STYLE
#  define FINISHER_STYLE 1           /* 0 = sifive-test, 1 = htif-like ((code<<1)|1) */
#endif

#define UART_TXDATA   (*(volatile uint32_t*)(UART_BASE + 0x00))
#define UART_RXDATA   (*(volatile uint32_t*)(UART_BASE + 0x04))
#define UART_TXCTRL   (*(volatile uint32_t*)(UART_BASE + 0x08))
#define UART_RXCTRL   (*(volatile uint32_t*)(UART_BASE + 0x0C))
#define UART_IE       (*(volatile uint32_t*)(UART_BASE + 0x10))
#define UART_IP       (*(volatile uint32_t*)(UART_BASE + 0x14))
#define UART_DIV      (*(volatile uint32_t*)(UART_BASE + 0x18))

#define TXDATA_FULL   (1u << 31)
#define RXDATA_EMPTY  (1u << 31)

// ===== Finisher (tùy testbench/SoC) =====
#ifdef FINISHER_BASE
# define FIN_REG   (*(volatile uint32_t*)(FINISHER_BASE + 0x0))
// Chọn 1 trong 2 encode tuỳ testbench.
// HTIF-like (thường gặp trong riscv-tests & nhiều harness):
# ifndef FINISHER_STYLE   // 0: sifive-test simple, 1: htif-like (mặc định dùng 1)
#  define FINISHER_STYLE  1
# endif
#endif

// ===== Minimal heap (sbrk) =====
extern char _end[];   // từ linker script (heap start)
static char* brk_ptr = _end;

caddr_t _sbrk(int incr) {
  // Không có giới hạn cụ thể ở đây — tùy bạn thêm guard theo RAM end.
  char* prev = brk_ptr;
  brk_ptr += incr;
  return (caddr_t)prev;
}

// ===== UART init (optional) =====
static inline void uart_init_default(void) {
  // Bạn có thể set baud: DIV = (freq/baud)-1; nếu không rõ clock, có thể để nguyên.
  // Tối thiểu bật TX/RX (bit0 = enable).
  UART_TXCTRL = 1u;  // txen
  UART_RXCTRL = 1u;  // rxen
  UART_IE     = 0u;  // no IRQ
}

// Gọi hàm này rất sớm (trước printf đầu tiên). Có thể gọi từ crt0 hoặc main().
__attribute__((weak))
void sys_io_init(void) { uart_init_default(); }

// ===== Syscalls =====

ssize_t _write(int fd, const void *buf, size_t len) {
  if (fd != STDOUT_FILENO && fd != STDERR_FILENO) {
    errno = EBADF;
    return -1;
  }
  const uint8_t* p = (const uint8_t*)buf;
  for (size_t i = 0; i < len; i++) {
    // Chuyển '\n' -> "\r\n" nếu muốn terminal đẹp (tùy chọn)
    uint8_t ch = p[i];
    if (ch == '\n') {
      // chèn '\r'
      while (UART_TXDATA & TXDATA_FULL) { }
      UART_TXDATA = (uint32_t)'\r';
    }
    while (UART_TXDATA & TXDATA_FULL) { }
    UART_TXDATA = (uint32_t)ch;
  }
  return (ssize_t)len;
}

ssize_t _read(int fd, void *buf, size_t len) {
  if (fd != STDIN_FILENO) {
    errno = EBADF;
    return -1;
  }
  uint8_t* p = (uint8_t*)buf;
  size_t n = 0;

  // Ở đây làm non-blocking: trả về 0 nếu không có dữ liệu.
  // Nếu muốn blocking, đổi thành: while (n < len) { đợi tới khi RX có data... }
  while (n < len) {
    uint32_t v = UART_RXDATA;
    if (v & RXDATA_EMPTY) break;   // không có data
    p[n++] = (uint8_t)(v & 0xFF);
  }
  return (ssize_t)n;
}

void _exit(int code) {
#ifdef FINISHER_BASE
  // Hai kiểu encode thường gặp:
  #if FINISHER_STYLE == 1
    // HTIF-like: ((code << 1) | 1)
    uint32_t v = ((uint32_t)code << 1) | 1u;
    FIN_REG = v;
  #else
    // Sifive-test kiểu đơn giản: 0x5555 = pass, các giá trị khác = fail (tuỳ harness).
    // Ở đây: 0 -> PASS, !=0 -> FAIL. Điều chỉnh theo testbench của bạn.
    FIN_REG = (code == 0) ? 0x5555u : (0x3333u | ((uint32_t)code & 0xFFFF));
  #endif
  // Chờ host dừng mô phỏng. Nếu không có host, park CPU.
  for (;;) { __asm__ volatile ("wfi"); }
#else
  // Không có finisher → park CPU để không chạy lung tung.
  for (;;) { __asm__ volatile ("wfi"); }
#endif
}

// ===== Các syscall “bắt buộc” tối thiểu cho newlib =====

int _close(int fd) {
  (void)fd; errno = ENOSYS; return -1;
}

int _fstat(int fd, struct stat *st) {
  (void)fd;
  if (!st) { errno = EINVAL; return -1; }
  st->st_mode = S_IFCHR;  // character device cho stdin/out/err
  return 0;
}

int _isatty(int fd) {
  // stdin/out/err → tty
  return (fd == STDIN_FILENO || fd == STDOUT_FILENO || fd == STDERR_FILENO) ? 1 : 0;
}

off_t _lseek(int fd, off_t offset, int whence) {
  (void)fd; (void)offset; (void)whence; errno = ENOSYS; return (off_t)-1;
}
