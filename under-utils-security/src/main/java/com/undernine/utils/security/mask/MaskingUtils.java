package com.undernine.utils.security.mask;

/**
 * 响应脱敏工具。
 *
 * @author Under-Utils Team
 * @version 1.0.4
 * @since 1.0.4
 */
public final class MaskingUtils {

    private static final String MASK_CHAR = "*";

    private MaskingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String mask(String value, MaskType type) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return switch (type) {
            case MOBILE_PHONE -> mobilePhone(value);
            case ID_CARD -> idCard(value);
            case BANK_CARD -> bankCard(value);
            case EMAIL -> email(value);
            case CHINESE_NAME -> chineseName(value);
            case ADDRESS -> address(value);
            case PASSWORD -> password(value);
            case FIXED_PHONE -> fixedPhone(value);
            case CAR_LICENSE -> carLicense(value);
            case CUSTOM -> value;
        };
    }

    public static String custom(String value, String customRule) {
        if (value == null || value.isEmpty() || customRule == null || customRule.isEmpty()) {
            return value;
        }
        String[] parts = customRule.split(",");
        if (parts.length != 2) {
            return value;
        }
        try {
            return mask(value, Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException ex) {
            return value;
        }
    }

    public static String mobilePhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return mask(phone, 3, 4);
    }

    public static String idCard(String idCard) {
        if (idCard == null || (idCard.length() != 15 && idCard.length() != 18)) {
            return idCard;
        }
        return mask(idCard, 3, 4);
    }

    public static String bankCard(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) {
            return cardNo;
        }
        return mask(cardNo, 4, 4);
    }

    public static String email(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int index = email.indexOf("@");
        String prefix = email.substring(0, index);
        if (prefix.length() <= 1) {
            return email;
        }
        return prefix.charAt(0) + MASK_CHAR.repeat(3) + email.substring(index);
    }

    public static String chineseName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        return name.charAt(0) + MASK_CHAR.repeat(name.length() - 1);
    }

    public static String address(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        int maskLength = Math.max(address.length() - 6, 11);
        return address.substring(0, 6) + MASK_CHAR.repeat(maskLength);
    }

    public static String password(String password) {
        return password == null ? null : MASK_CHAR.repeat(3);
    }

    public static String fixedPhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.contains("-")) {
            String[] parts = phone.split("-");
            if (parts.length == 2 && parts[1].length() >= 4) {
                return parts[0] + "-" + mask(parts[1], 0, 4);
            }
        }
        return mask(phone, 3, 3);
    }

    public static String carLicense(String license) {
        if (license == null || license.length() < 7) {
            return license;
        }
        return mask(license, 3, 1);
    }

    private static String mask(String value, int prefixLen, int suffixLen) {
        if (value == null) {
            return null;
        }
        if (prefixLen < 0 || suffixLen < 0 || value.length() <= prefixLen + suffixLen) {
            return value;
        }
        String prefix = value.substring(0, prefixLen);
        String suffix = value.substring(value.length() - suffixLen);
        return prefix + MASK_CHAR.repeat(value.length() - prefixLen - suffixLen) + suffix;
    }
}
