package tech.kikutaro;

import java.io.IOException;
import java.util.List;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import io.github.cdimascio.dotenv.Dotenv;
import notion.api.v1.NotionClient;
import notion.api.v1.model.blocks.Blocks;
import notion.api.v1.model.blocks.TableBlock;
import notion.api.v1.model.blocks.TableRowBlock;
import notion.api.v1.model.pages.PageProperty.RichText;

public final class App {
    public static void main(String[] args) throws IOException {
        Dotenv env = Dotenv.load();
        try (NotionClient client = new NotionClient(env.get("NTOKEN"))) {
            //メールの基本情報
            Mail msg = new Mail();
            msg.setSubject("nameさん宛てのメールです");
            msg.setFrom(new Email("from@example.com"));
            msg.addContent(new Content("text/plain", "name さん こんにちは。"));
            
            //ページからTableを取得
            Blocks pageBlocks = client.retrieveBlockChildren(env.get("PAGEID"), null, 0);
            TableBlock table = pageBlocks.getResults().get(0).asTable();
            Blocks rows = client.retrieveBlockChildren(table.getId(), null, 0);

            //Tableの内容に基づいて差し込み
            rows.getResults().stream().forEach(r -> {
                TableRowBlock tbr = r.asTableRow();
                Personalization p = new Personalization();
                for(int i=0; i < tbr.getTableRow().getCells().size(); i++) {
                    List<RichText> list = tbr.getTableRow().getCells().get(i);
                    if(i%2==0) {
                        p.addTo(new Email(list.get(0).getPlainText()));
                    } else {
                        p.addSubstitution("name", list.get(0).getPlainText());
                    }
                }
                msg.addPersonalization(p);
            });

            //送信処理
            SendGrid sg = new SendGrid(env.get("SGKEY"));
            Request req = new Request();
            req.setMethod(Method.POST);
            req.setEndpoint("mail/send");
            req.setBody(msg.build());
            Response resp = sg.api(req);
            System.out.println(resp.getStatusCode());
        }
    }
}
