package cx.shapefile;

import java.util.HashMap;
import java.util.Map;

public class LeeCode
{
    public static String decodeMessage(String key, String message) {
        char[] alphabet = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
        Map<Character,Character> reflectItem = new HashMap<>();
        reflectItem.put(' ',' ');
        for (int i = 0, j = 0; i < key.length(); i++)
        {
            if(!reflectItem.containsKey(key.charAt(i)))
            {
                reflectItem.put(key.charAt(i),alphabet[j++]);
            }
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < message.length(); i++)
        {
            result.append(reflectItem.get(message.charAt(i)));
        }
        return result.toString();
    }

    public static void main(String[] args)
    {
        String k = "the quick brown fox jumps over the lazy dog";
        String m = "vkbs bs t suepuv";
        String s = decodeMessage(k, m);
        System.out.println(s);
    }
}
