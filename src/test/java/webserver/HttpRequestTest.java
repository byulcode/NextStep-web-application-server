package webserver;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestTest {
    private String testDirectory = "./src/test/resources/";

    @Test
    void request_GET() throws Exception{
        InputStream in = new FileInputStream(new File(testDirectory + "Http_GET.txt"));
        HttpRequest request = new HttpRequest(in);

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/user/create");
        assertThat(request.getHeater("Connection")).isEqualTo("keep-alive");
        assertThat(request.getParameter("userId")).isEqualTo("byulcode");
    }

}