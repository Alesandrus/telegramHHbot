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

public class Bot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LogManager.getLogger(Bot.class.getName());

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        if (msg != null && msg.hasText()) {
            String cmdName = msg.getText();
            if (cmdName.equals("/start")) {
                sendMsg(msg, "Юляшка приветяшка");
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
    private void sendMsg(Message msg, String text) {
        SendMessage message = new SendMessage();
        //message.enableMarkdown(true);
        message.setChatId(msg.getChatId());
        //message.setReplyToMessageId(msg.getMessageId());
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
