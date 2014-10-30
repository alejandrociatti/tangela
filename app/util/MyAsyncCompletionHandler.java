package util;

import com.ning.http.client.Response;
import com.ning.http.client.AsyncCompletionHandler;

/**
 * Created with IntelliJ IDEA by: alejandro
 * Date: 29/10/14
 * Time: 16:58
 */
public class MyAsyncCompletionHandler{
    public static AsyncCompletionHandler MY_HANDLER = new AsyncCompletionHandler() {
        @Override
        public Object onCompleted(Response response) throws Exception {
            final String body = response.getResponseBody();
            if(body.contains("<h1 class=\"on\">") || body.contains("<h1 class=\"not\">")){
                System.out.println("NING is using the TOR network.");
            }else{
                System.out.println("NING is not under the TOR network");
            }
            return response;
        }
    };
}
