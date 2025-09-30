// htif_fake.c — Fake host để không bao giờ busy-wait fromhost
#include <stdint.h>

// Mã syscall theo libgloss-htif: 64=write, 93=exit, 214=brk
#define SYS_write 64u
#define SYS_exit  93u
#define SYS_brk   214u

// Wrap htif_syscall: thay vì ghi tohost rồi đợi fromhost, trả kết quả ngay
long __wrap_htif_syscall(uint64_t a0, uint64_t a1, uint64_t a2, unsigned long n)
{
    (void)a0; (void)a1;
    switch (n) {
    case SYS_write:
        // giả sử ghi thành công: trả số byte đã “ghi”
        return (long)a2;
    case SYS_brk:
        // báo thành công
        return 0;
    case SYS_exit:
        // KHÔNG dùng wfi: park bằng vòng lặp “j .”
        for (;;) { __asm__ volatile ("j ."); }
    default:
        // không hỗ trợ: trả lỗi chung
        return -1;
    }
}

// Phòng khi có code gọi trực tiếp _write/_exit (không đi qua htif_syscall)
int  __wrap__write(int fd, const void *buf, unsigned len) {
    (void)fd; (void)buf; return (int)len;
}
void __wrap__exit(int code) {
    (void)code;
    for (;;) { __asm__ volatile ("j ."); }  // không wfi
}

