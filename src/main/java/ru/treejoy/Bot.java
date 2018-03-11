package ru.treejoy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Бот обновляющий вакансию на hh.ru.
 */
public class Bot extends TelegramLongPollingBot {
    /**
     * Токен бота.
     */
    private String botToken;

    /**
     * Логгер.
     */
    private static final Logger LOGGER = LogManager.getLogger(Bot.class.getName());

    /**
     * Мэпа апдейтеров для каждого чата.
     */
    private Map<Long, Updater> map = new ConcurrentHashMap<>();

    /**
     * Конструктор.
     *
     * @param botToken токен.
     */
    public Bot(String botToken) {
        this.botToken = botToken;
    }

    /**
     * Получение введенной информации.
     *
     * @param update update.
     */
    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        if (msg != null && msg.hasText()) {
            String cmdName = msg.getText();
            Long chatId = msg.getChatId();
            if (!map.containsKey(chatId)) {
                map.put(chatId, new Updater(243, TimeUnit.MINUTES, this, chatId));
            }
            if (map.get(chatId).isCheckResume()) {
                map.get(chatId).setResumeID(cmdName);
                map.get(chatId).setCheckResume(false);
            } else if (map.get(chatId).isCheckToken()) {
                map.get(chatId).setTokenHH(cmdName);
                map.get(chatId).setCheckToken(false);
            } else {
                if (cmdName.equals("/start")) {
                    if (map.get(chatId).getResumeID() == null) {
                        sendMsg(msg, "Укажите ID вашего резюме на hh.ru c помощью команды /resume_id");
                    } else if (map.get(chatId).getTokenHH() == null) {
                        sendMsg(msg, "Укажите ваш токен с dev.hh.ru/admin c помощью команды /token_hh");
                    } else {
                        map.get(chatId).executeUpdate();
                    }
                } else if (cmdName.equals("/stop")) {
                    if (map.containsKey(chatId)) {
                        map.get(chatId).shutdown();
                    }
                } else if (cmdName.equals("/resume_id")) {
                    map.get(chatId).setCheckResume(true);
                    sendMsg(msg, "Введите ID");
                } else if (cmdName.equals("/token_hh")) {
                    map.get(chatId).setCheckToken(true);
                    sendMsg(msg, "Введите токен");
                }
            }
        }
    }

    /**
     * Получить имя бота.
     *
     * @return имя бота.
     */
    @Override
    public String getBotUsername() {
        return "Shelby";
    }

    /**
     * Получить токен бота.
     *
     * @return токен.
     */
    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Послать сообщение.
     *
     * @param msg  Message.
     * @param text текст.
     */
    @SuppressWarnings("deprecation")
    private void sendMsg(Message msg, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(msg.getChatId());
        message.setText(text);
        try {
            sendMessage(message);
        } catch (TelegramApiException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Main.
     *
     * @param args token.
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            LOGGER.info("Start main");
            ApiContextInitializer.init();
            TelegramBotsApi botsApi = new TelegramBotsApi();
            try {
                botsApi.registerBot(new Bot(args[0]));
            } catch (TelegramApiRequestException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
