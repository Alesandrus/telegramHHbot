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

/**
 * Класс для обновления резюме.
 */
public class Updater {
    /**
     * Логгер.
     */
    private static final Logger LOGGER = LogManager.getLogger(Bot.class.getName());

    /**
     * Экзекьютер.
     */
    private ScheduledExecutorService executorService;

    /**
     * ID резюме.
     */
    private String resumeID;

    /**
     * Токен личного кабинета hh.ru.
     */
    private String tokenHH;

    /**
     * Период между обращением к hh.ru для обновления.
     */
    private long period;

    /**
     * TimeUnit.
     */
    private TimeUnit timeUnit;

    /**
     * Бот.
     */
    private Bot bot;

    /**
     * ID чата.
     */
    private Long chatID;

    /**
     * Помогает определить что введенные данные это ID резюме.
     */
    private boolean checkResume;

    /**
     * Помогает определить что введенные данные это токен hh.ru.
     */
    private boolean checkToken;

    /**
     * Конструктор.
     *
     * @param period   период.
     * @param timeUnit TimeUnit.
     * @param bot      бот.
     * @param chatID   ID чата.
     */
    public Updater(long period, TimeUnit timeUnit, Bot bot, Long chatID) {
        this.period = period;
        this.timeUnit = timeUnit;
        this.bot = bot;
        this.chatID = chatID;
    }

    /**
     * Выполнить обновление резюме.
     */
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
                    checkResume = false;
                    checkToken = false;
                    executorService.shutdown();
                /*} else if (statusCode == 429) {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatID);
                    message.setText("Резюме обновится через некоторое время");
                    try {
                        bot.sendMessage(message);
                    } catch (TelegramApiException e) {
                        LOGGER.error(e.getMessage(), e);
                    }*/
                } else if (statusCode == 204) {
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

    /**
     * Остановить выполнение обновления.
     */
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

    /**
     * Геттер checkResume.
     *
     * @return true если начали вводить /resume_id.
     */
    public boolean isCheckResume() {
        return checkResume;
    }

    /**
     * Сеттер checkResume.
     *
     * @param checkResume .
     */
    public void setCheckResume(boolean checkResume) {
        this.checkResume = checkResume;
    }

    /**
     * Геттер checkToken.
     *
     * @return true если начали вводить /token_hh.
     */
    public boolean isCheckToken() {
        return checkToken;
    }

    /**
     * Сеттер checkToken.
     *
     * @param checkToken .
     */
    public void setCheckToken(boolean checkToken) {
        this.checkToken = checkToken;
    }

    /**
     * Геттер ID резюме.
     *
     * @return ID резюме.
     */
    public String getResumeID() {
        return resumeID;
    }

    /**
     * Сеттер ID резюме.
     *
     * @param resumeID .
     */
    public void setResumeID(String resumeID) {
        this.resumeID = resumeID;
    }

    /**
     * Геттер токена.
     *
     * @return токен.
     */
    public String getTokenHH() {
        return tokenHH;
    }

    /**
     * Сеттер токена.
     *
     * @param tokenHH .
     */
    public void setTokenHH(String tokenHH) {
        this.tokenHH = tokenHH;
    }
}
