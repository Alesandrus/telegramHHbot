package ru.treejoy;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Updater {
    private static final Logger LOGGER = LogManager.getLogger(Bot.class.getName());
    private ScheduledExecutorService executorService;
    private String resumeID;
    private String tokenHH;
    private long period;
    private TimeUnit timeUnit;
    private Bot bot;
    private Long chatID;
    private boolean setResume;
    private boolean setToken;

    public Updater(long period, TimeUnit timeUnit, Bot bot, Long chatID) {
        this.period = period;
        this.timeUnit = timeUnit;
        this.bot = bot;
        this.chatID = chatID;
    }

    public void executeUpdate() {
        HttpPost httpPost = new HttpPost(String.format("https://api.hh.ru/resumes/%s/publish", resumeID));
        httpPost.addHeader("User-Agent", "api-test-agent");
        httpPost.addHeader("Authorization", "Bearer " + tokenHH);
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                LOGGER.info(statusCode);
                if (statusCode == 403) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatID);
                    message.setText("Проверьте срок действия токена,"
                            + " или корректно ли ввели вы ID вашего резюме и токен");
                    try {
                        bot.sendMessage(message);
                    } catch (TelegramApiException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                    setResume = false;
                    setToken = false;
                    executorService.shutdown();
                } /*else if (statusCode == 429) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatID);
                    message.setText("Резюме обновится через некоторое время");
                    try {
                        bot.sendMessage(message);
                    } catch (TelegramApiException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                } */else if (statusCode == 204) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatID);
                    message.setText("Резюме обновлено");
                    try {
                        bot.sendMessage(message);
                    } catch (TelegramApiException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }, 0, period, timeUnit);
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
            SendMessage message = new SendMessage();
            message.setChatId(chatID);
            message.setText("Автообновление остановлено");
            try {
                bot.sendMessage(message);
            } catch (TelegramApiException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public boolean isSetResume() {
        return setResume;
    }

    public void setSetResume(boolean setResume) {
        this.setResume = setResume;
    }

    public boolean isSetToken() {
        return setToken;
    }

    public void setSetToken(boolean setToken) {
        this.setToken = setToken;
    }

    public String getResumeID() {
        return resumeID;
    }

    public void setResumeID(String resumeID) {
        this.resumeID = resumeID;
    }

    public String getTokenHH() {
        return tokenHH;
    }

    public void setTokenHH(String tokenHH) {
        this.tokenHH = tokenHH;
    }
}
