package se.anyro.chatbridge;

import java.io.IOException;

import se.anyro.tgbotapi.TgBotApi;
import se.anyro.tgbotapi.types.Chat;
import se.anyro.tgbotapi.types.Message;
import se.anyro.tgbotapi.types.ParseMode;
import se.anyro.tgbotapi.types.User;

/**
 * Handler for a single message sent to the bot.
 */
public class MessageHandler {

    private TgBotApi api;
    private Message message;

    private static Message lastMessage = new Message();
    {
        lastMessage.chat = new Chat(); // Dummy chat to prevent null pointer
    }

    public MessageHandler(TgBotApi api, Message message) {
        this.api = api;
        this.message = message;
    }

    public void run() throws IOException {
        // Rudimentary flood control
        if (message.chat.id == lastMessage.chat.id) {
            if (message.date - lastMessage.date < 4) {
                lastMessage = message;
                return;
            }
        }
        lastMessage = message;

        if (message.isReply()) {
            forwardToReceipient();
        } else if (message.text.startsWith("/link")) {
            sendStartLink();
        } else if (message.text.startsWith("/start ")) {
            String recipientId = message.text.substring(7);
            startChat(recipientId);
        } else if (message.text.startsWith("/start") || message.text.startsWith("/help")) {
            sendWelcomeText();
        } else if (message.forward_from == null) {
            api.sendMessage(
                    message.chat.id,
                    "You need to reply to a forwarded message. If you have no message to reply to, you can forward a message here first from a person you want to chat with.");
        }
    }

    private void forwardToReceipient() throws IOException {
        User recipient = message.reply_to_message.forward_from;
        if (recipient != null) {
            int responseCode = api.forwardMessage(recipient.id, message);
            if (responseCode == 400 || responseCode == 403) {
                api.sendMessage(message.chat.id, recipient.first_name
                        + " is not using @chatbridgebot at the moment and will not receive your message.");
            } else if (responseCode >= 300) {
                api.sendMessage(message.chat.id, "Error " + responseCode + " when sending message to "
                        + recipient.first_name);
            }
        } else {
            api.sendReply(message, "You should only reply to forwarded messages");
        }
    }

    private void sendStartLink() throws IOException {
        api.sendMessage(message.chat.id, createStartLink(), null, true, 0, null);
    }

    private void startChat(String recipientId) throws IOException {
        if (recipientId.equals(String.valueOf(message.from.id))) {
            api.sendMessage(recipientId,
                    "You are not supposed to use your own start link. You should send it to someone else.");
        } else {
            api.sendMessage(recipientId, message.from.first_name
                    + " wants to chat with you. To start chatting just reply to the next message...");
            api.forwardMessage(recipientId, message);
            api.sendMessage(message.chat.id, "Message sent to user " + recipientId);
        }
    }

    private String createStartLink() {
        return "https://telegram.me/chatbridgebot?start=" + message.from.id;
    }

    private void sendWelcomeText() throws IOException {
        api.sendMessage(
                message.chat.id,
                "*Welcome to Chat Bridge Bot!*\n\nTo start messaging with your friend:\n"
                        + "1. Ask your friend to send `/start` to @chatbridgebot, if not done already\n2. Forward one of your friend's messages to @chatbridgebot\n3. Reply to that message and your friend will receive your reply\n4. Keep replying to your friend's messages\n\n"
                        + "...or...\n\n1. Send /link to get a personal start link\n2. Send the link to your friend\n3. Wait for a forwarded message from your friend\n4. Reply to your friend's messages\n"
                        + "\nبرای آغاز مکالمه با دوست خود:\n1. از او بخواهید که برای من @Chatbridgebot پیام '/start' را ارسال کند.\n2. یکی از پیام های دوستانتان را برای من فروارد کنید.\n3.به همان پیام ریپلای کنید و دوستتان آنرا دریافت خواهد کرد.\n4.پیام دادن را با ریپلای کردن ادامه دهید.\n\n...یا...\n\n1. فرمان /link را برای گرفتن لینک ارتباط شخصی ارسال کنید.\n2. لینک دریافتی را برای دوست خود ارسال کنید.\n3. منتظر یک پیام فرواردی از سوی دوست خود باشید.\n4. با ریپلای کردن به دوست خود پیام دهید.",
                ParseMode.MARKDOWN, true, 0, null);
    }
}