package com.hireshield.rules;

import java.util.Locale;
import java.util.Set;

public final class FreeEmailProviders {

    private static final Set<String> PROVIDERS = Set.of(
            "gmail.com", "googlemail.com", "yahoo.com", "yahoo.in", "yahoo.co.in", "ymail.com",
            "outlook.com", "hotmail.com", "live.com", "msn.com", "rediffmail.com", "rediff.com",
            "aol.com", "icloud.com", "me.com", "protonmail.com", "proton.me", "zoho.com",
            "zohomail.in", "mail.com", "gmx.com", "yandex.com", "tutanota.com");

    private FreeEmailProviders() {
    }

    public static boolean isFree(String domain) {
        return domain != null && PROVIDERS.contains(domain.toLowerCase(Locale.ROOT));
    }
}
