package cx.shapefile.utils.cx;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

public class SvrUtils
{
    public static byte[] getRequest(String path)
    {
        HttpRequest request = HttpRequest.get(path);
        HttpResponse response = request.send();
        return response.bodyBytes();
    }
}
