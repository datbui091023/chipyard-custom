#include <sys/stat.h>
#include <stdint.h>
#include <errno.h>
#include <stddef.h>

extern char _end;             // từ linker: PROVIDE(end = _end);
static char *brk = &_end;
#ifndef S_IFCHR
#  define S_IFCHR 0020000   /* fallback nếu header không định nghĩa */
#endif
int _write(int fd, const void *buf, unsigned len) {
  (void)fd; (void)buf;
  // Nếu bạn có UART, hãy map ra UART ở đây.
  // Tạm thời: giả vờ ghi thành công
  return (int)len;
}

int _read(int fd, void *buf, unsigned len)      { (void)fd; (void)buf; (void)len; errno = ENOSYS; return -1; }
int _close(int fd)                               { (void)fd; errno = ENOSYS; return -1; }
int _lseek(int fd, int ptr, int dir)            { (void)fd; (void)ptr; (void)dir; errno = ENOSYS; return -1; }
int _isatty(int fd)                              { (void)fd; return 1; }
int _fstat(int fd, struct stat *st)              { (void)fd; st->st_mode = S_IFCHR; return 0; }
int _kill(int pid, int sig)                      { (void)pid; (void)sig; errno = ENOSYS; return -1; }
int _getpid(void)                                { return 1; }

void* _sbrk(ptrdiff_t inc) {
  char *prev = brk;
  brk += inc;
  return prev;
}
