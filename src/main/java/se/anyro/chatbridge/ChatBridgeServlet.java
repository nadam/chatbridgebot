package se.anyro.chatbridge;

import static se.anyro.chatbridge.BuildVars.OWNER;
import static se.anyro.chatbridge.BuildVars.TOKEN;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import se.anyro.tgbotapi.TgBotApi;
import se.anyro.tgbotapi.TgBotApi.ErrorListener;
import se.anyro.tgbotapi.types.Update;
import se.anyro.tgbotapi.types.User;
import se.anyro.tgbotapi.types.inline.InlineQuery;
import se.anyro.tgbotapi.types.inline.InlineQueryResult;
import se.anyro.tgbotapi.types.inline.InlineQueryResultArticle;
import se.anyro.tgbotapi.types.inline.InputTextMessageContent;

/**
 * Bot that enables chatting by forwarding message replies.
 */
@SuppressWarnings("serial")
public class ChatBridgeServlet extends HttpServlet implements ErrorListener {

    private TgBotApi api;
    
    public ChatBridgeServlet() {
        super();
        try {
            api = new TgBotApi(TOKEN, OWNER, this);
            api.sendMessage(OWNER, "Bot started");
        } catch (IOException e) {
            log("Error setting up bot", e);
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setStatus(200);

        Update update = api.parseFromWebhook(req.getReader());
        try {
            if (update.isMessage()) {
                new MessageHandler(api, update.message).run();
            } else if (update.isInlineQuery()) {
                handleInlineQuery(update.inline_query);
            }
        } catch (Exception e) {
            // Simplify testing for the owner of the bot
            if (api.isOwner(update.fromUser())) {
                api.debug(e);
            }
        }
    }

    /**
     * In inline mode the bot lets you post your private start link which others can use.
     */
    private void handleInlineQuery(InlineQuery query) {
        try {
            String message = "You can message me privately using @chatbridgebot\n" + createStartLink(query.from);
            InputTextMessageContent content = new InputTextMessageContent(message, null, true);
            content.disable_web_page_preview = true;
            InlineQueryResult[] results = { new InlineQueryResultArticle("0", message, content) };
            api.answerInlineQuery(query.id, results, true);
        } catch (IOException e) {
            if (api.isOwner(query.from)) {
                api.debug(e);
            }
        }
    }

    private String createStartLink(User user) {
        return "https://telegram.me/chatbridgebot?start=" + user.id;
    }

    @Override
    public void onError(int errorCode, String description) {
        // Ignore errors where the other user is not using the bot
        if (errorCode != 400 && errorCode != 403) {
            api.debug(new Exception("ErrorCode " + errorCode + ", " + description));
        }
    }
}