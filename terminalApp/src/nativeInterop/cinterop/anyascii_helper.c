#include <string.h>
#include "anyascii.h"
#include "anyascii_helper.h"

size_t anyascii_render(uint_least32_t utf32, char *out, size_t out_cap) {
    const char *src;
    size_t n = anyascii(utf32, &src);
    if (n > out_cap - 1) n = out_cap - 1;
    memcpy(out, src, n);
    out[n] = '\0';
    return n;
}
