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

public class Bot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LogManager.getLogger(Bot.class.getName());
    private Map<Long, Updater> map = new ConcurrentHashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        if (msg != null && msg.hasText()) {
            String cmdName = msg.getText();
            Long chatId = msg.getChatId();
            if (!map.containsKey(chatId)) {
                map.put(chatId, new Updater(243, TimeUnit.MINUTES, this, chatId));
            }
            if (map.get(chatId).isSetResume()) {
                map.get(chatId).setResumeID(cmdName);
                map.get(chatId).setSetResume(false);
            } else if (map.get(chatId).isSetToken()) {
                map.get(chatId).setTokenHH(cmdName);
                map.get(chatId).setSetToken(false);
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
                    map.get(chatId).setSetResume(true);
                    sendMsg(msg, "Введите ID");
                } else if (cmdName.equals("/token_hh")) {
                    map.get(chatId).setSetToken(true);
                    sendMsg(msg, "Введите токен");
                }
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "Shelby";
    }

    @Override
    public String getBotToken() {
        return "502626763:AAFZWXNzklFlFQ5Oe_7XbECedkZO3FEbEG0";
    }

    @SuppressWarnings("deprecation")
    public void sendMsg(Message msg, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(msg.getChatId());
        message.setText(text);
        try {
            sendMessage(message);
        } catch (TelegramApiException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        LOGGER.info("Start main");
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        try {
            botsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
