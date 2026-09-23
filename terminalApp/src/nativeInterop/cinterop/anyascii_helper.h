#ifndef ANYASCII_HELPER_H
#define ANYASCII_HELPER_H

#include <stdint.h>
#include <stddef.h>

/**
 * Wrapper that returns anyascii transliteration in a pre-allocated buffer.
 * `out` must be at least 16 bytes. The result is null-terminated.
 * Returns the number of chars written (excluding null terminator).
 */
size_t anyascii_render(uint_least32_t utf32, char *out, size_t out_cap);

#endif
