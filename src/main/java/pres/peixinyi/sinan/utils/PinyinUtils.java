package pres.peixinyi.sinan.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

/**
 * 拼音转换工具类
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/23
 * @Version : 1.0.0
 */
public class PinyinUtils {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        // 设置拼音格式：小写、无音调、u显示为u
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    /**
     * 将中文转换为拼音
     *
     * @param chinese 中文字符串
     * @return 拼音字符串，非中文字符保持原样
     */
    public static String toPinyin(String chinese) {
        if (chinese == null || chinese.trim().isEmpty()) {
            return "";
        }

        StringBuilder pinyin = new StringBuilder();
        char[] chars = chinese.toCharArray();

        for (char ch : chars) {
            if (Character.toString(ch).matches("[\\u4E00-\\u9FA5]+")) {
                // 是中文字符
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]);
                    } else {
                        pinyin.append(ch);
                    }
                } catch (Exception e) {
                    // 转换失败，保留原字符
                    pinyin.append(ch);
                }
            } else {
                // 非中文字符，直接添加
                pinyin.append(ch);
            }
        }

        return pinyin.toString();
    }

    /**
     * 将中文转换为拼音首字母
     *
     * @param chinese 中文字符串
     * @return 拼音首字母字符串
     */
    public static String toPinyinFirstLetter(String chinese) {
        if (chinese == null || chinese.trim().isEmpty()) {
            return "";
        }

        StringBuilder firstLetters = new StringBuilder();
        char[] chars = chinese.toCharArray();

        for (char ch : chars) {
            if (Character.toString(ch).matches("[\\u4E00-\\u9FA5]+")) {
                // 是中文字符
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        firstLetters.append(pinyinArray[0].charAt(0));
                    } else {
                        firstLetters.append(ch);
                    }
                } catch (Exception e) {
                    // 转换失败，保留原字符
                    firstLetters.append(ch);
                }
            } else if (Character.isLetter(ch)) {
                // 是英文字母，取首字母
                firstLetters.append(Character.toLowerCase(ch));
            }
        }

        return firstLetters.toString();
    }
}
