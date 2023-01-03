package webserver;

import java.io.*;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // 요청정보 라인별로 읽기
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = br.readLine();
            log.debug("request : {}", line);

            // 요청 url 추출하기
            String[] tokens = line.split(" ");
            String url = tokens[1];

            // null일 경우 예외처리
            if (line == null) {
                return;
            }

            // Content-Length 값 가져오기
            int contentLength = 0;
            while (!"".equals(line)) {
                line = br.readLine();
                if (line.contains("Content-Length")) {
                    String[] content = line.split(":");
                    contentLength = Integer.parseInt(content[1].trim());
                }
                log.debug("line : {}", line);
            }

            DataOutputStream dos = new DataOutputStream(out);

            // 회원가입시 body에 저장된 회원정보 읽기
            if (url.equals("/user/create")) {
                String body = IOUtils.readData(br, contentLength);
                Map<String, String> params = HttpRequestUtils.parseQueryString(body);
                User user = new User(params.get("userId"), params.get("password"),
                        params.get("name"), params.get("email"));
                log.debug("User : {}", user);
                //database에 user 저장
                DataBase.addUser(user);

                // /index.html로 리다이렉트
                response302Header(dos, "/index.html");

            }  //로그인시 body에 저장된 로그인정보 읽기
            else if (url.equals("/user/login")) {
                String body = IOUtils.readData(br, contentLength);
                Map<String, String> params = HttpRequestUtils.parseQueryString(body);
                User user = DataBase.findUserById(params.get("userId"));

                // 로그인 정보 확인 및 쿠키 설정
                String cookie = "";
                //불러온 정보에 해당하는 회원이 없을 경우
                if (user == null) {
                    byte[] resBody = Files.readAllBytes(new File("./webapp/user/login_failed.html").toPath());
                    response200HeaderLoginFalse(dos, resBody.length);
                }
                // 아이디가 존재할 경우
                else {
                    if (user.getPassword().equals(params.get("password"))) { //비밀번호가 맞은 경우
                        url = "/index.html";
                        cookie = "logined=true";
                    } else {//비밀번호가 틀린 경우
                        byte[] resBody = Files.readAllBytes(new File("./webapp/user/login_failed.html").toPath());
                        response200HeaderLoginFalse(dos, resBody.length);
                    }
                }
                response302HeaderWithCookie(dos, url, cookie);
            } else {
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 FOUND \r\n");
            dos.writeBytes("Location: " + url);
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithCookie(DataOutputStream dos, String url, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 FOUND \r\n");
            dos.writeBytes("Location: /index.html" + "\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200HeaderLoginFalse(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("Set-Cookie: logined=false");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
