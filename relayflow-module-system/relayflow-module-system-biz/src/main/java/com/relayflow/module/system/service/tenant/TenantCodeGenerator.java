package com.relayflow.module.system.service.tenant;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.mapper.SysTenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates globally unique opaque enterprise codes (Feishu-style).
 */
@Component
@RequiredArgsConstructor
public class TenantCodeGenerator {

    /** Exclude ambiguous 0/O/1/I. */
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 11;
    private static final int MAX_ATTEMPTS = 32;

    private final SysTenantMapper tenantMapper;
    private final SecureRandom random = new SecureRandom();

    public String nextUniqueCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            Long count = tenantMapper.selectCount(Wrappers.<SysTenantDO>lambdaQuery()
                    .eq(SysTenantDO::getCode, code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to allocate unique tenant code after " + MAX_ATTEMPTS + " attempts");
    }

    private String randomCode() {
        char[] chars = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            chars[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }
}
