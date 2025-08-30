package pres.peixinyi.sinan.utils;

import java.util.Random;

/**
 * 随机中文名称生成工具类
 * 生成类似"开心的小狗"这样的中文名称
 *
 * @Author : PeiXinyi
 * @Date : 2025/8/28 19:35
 * @Version : 0.0.0
 */
public class NameUtils {

    private static final Random RANDOM = new Random();

    /**
     * 形容词数组
     */
    private static final String[] ADJECTIVES = {
            "开心", "快乐", "聪明", "可爱", "温柔", "勇敢", "善良", "活泼",
            "机智", "优雅", "美丽", "帅气", "酷炫", "神秘", "安静", "淘气",
            "乖巧", "顽皮", "灵活", "敏捷", "强壮", "小巧", "胖胖", "瘦瘦",
            "高大", "迷你", "精致", "朴实", "华丽", "简约", "古典", "现代",
            "清新", "甜美", "阳光", "温暖", "清凉", "火热", "冰冷", "柔软"
    };

    /**
     * 动物数组
     */
    private static final String[] ANIMALS = {
            "小狗", "小猫", "小兔", "小鸟", "小鱼", "小熊", "小猪", "小羊",
            "小马", "小牛", "小鸡", "小鸭", "小鹅", "小老鼠", "小猴", "小象",
            "小虎", "小狮", "小狼", "小狐", "小鹿", "小熊猫", "小企鹅", "小海豚",
            "小章鱼", "小螃蟹", "小虾", "小蝴蝶", "小蜜蜂", "小蜻蜓", "小青蛙", "小乌龟"
    };

    /**
     * 植物数组
     */
    private static final String[] PLANTS = {
            "小花", "小草", "小树", "玫瑰", "百合", "向日葵", "郁金香", "荷花",
            "桃花", "樱花", "梅花", "兰花", "菊花", "牡丹", "茉莉", "薰衣草",
            "仙人掌", "竹子", "松树", "柳树", "梧桐", "银杏", "枫树", "橡树"
    };

    /**
     * 物品数组
     */
    private static final String[] OBJECTS = {
            "小星星", "小月亮", "小太阳", "小云朵", "小雪花", "小雨滴", "小石头", "小贝壳",
            "小珍珠", "小宝石", "小铃铛", "小蝴蝶结", "小帽子", "小书包", "小玩具", "小积木",
            "小汽车", "小飞机", "小船", "小火车", "小自行车", "小滑板", "小风筝", "小气球"
    };

    /**
     * 生成随机中文名称
     * 格式：形容词 + 的 + 名词
     *
     * @return 随机生成的中文名称，例如"开心的小狗"
     */
    public static String generateRandomName() {
        String adjective = getRandomElement(ADJECTIVES);
        String noun = getRandomNoun();
        return adjective + "的" + noun;
    }

    /**
     * 生成随机中文名称（指定类型）
     *
     * @param type 名词类型：animal(动物)、plant(植物)、object(物品)
     * @return 随机生成的中文名称
     */
    public static String generateRandomName(String type) {
        String adjective = getRandomElement(ADJECTIVES);
        String noun;

        switch (type.toLowerCase()) {
            case "animal":
                noun = getRandomElement(ANIMALS);
                break;
            case "plant":
                noun = getRandomElement(PLANTS);
                break;
            case "object":
                noun = getRandomElement(OBJECTS);
                break;
            default:
                noun = getRandomNoun();
                break;
        }

        return adjective + "的" + noun;
    }

    /**
     * 生成简单的随机名称（只有名词，不带形容词）
     *
     * @return 随机名词
     */
    public static String generateSimpleName() {
        return getRandomNoun();
    }

    /**
     * 生成简单的随机名称（指定类型）
     *
     * @param type 名词类型：animal(动物)、plant(植物)、object(物品)
     * @return 随机名词
     */
    public static String generateSimpleName(String type) {
        switch (type.toLowerCase()) {
            case "animal":
                return getRandomElement(ANIMALS);
            case "plant":
                return getRandomElement(PLANTS);
            case "object":
                return getRandomElement(OBJECTS);
            default:
                return getRandomNoun();
        }
    }

    /**
     * 获取随机名词（从所有类型中随机选择）
     *
     * @return 随机名词
     */
    private static String getRandomNoun() {
        // 随机选择一个类型
        int typeIndex = RANDOM.nextInt(3);
        switch (typeIndex) {
            case 0:
                return getRandomElement(ANIMALS);
            case 1:
                return getRandomElement(PLANTS);
            case 2:
                return getRandomElement(OBJECTS);
            default:
                return getRandomElement(ANIMALS);
        }
    }

    /**
     * 从数组中随机获取一个元素
     *
     * @param array 字符串数组
     * @return 随机元素
     */
    private static String getRandomElement(String[] array) {
        return array[RANDOM.nextInt(array.length)];
    }

    /**
     * 批量生成随机名称
     *
     * @param count 生成数量
     * @return 名称数组
     */
    public static String[] generateRandomNames(int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = generateRandomName();
        }
        return names;
    }

    /**
     * 批量生成随机名称（指定类型）
     *
     * @param count 生成数量
     * @param type 名词类型
     * @return 名称数组
     */
    public static String[] generateRandomNames(int count, String type) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) {
            names[i] = generateRandomName(type);
        }
        return names;
    }
}
