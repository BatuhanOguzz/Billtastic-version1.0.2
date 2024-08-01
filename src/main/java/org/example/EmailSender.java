package org.example;


import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.*;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class EmailSender {

    // Bu metot, verilen Excel dosyasından verileri okuyarak e-posta gönderir
    public void sendEmails(String excelFile, String username, String password) throws MessagingException {
        try (FileInputStream fileInputStream = new FileInputStream(excelFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream)) {

            // İlk sayfayı al
            XSSFSheet sheet = workbook.getSheetAt(0);

            // Mail ayarlarını yap
            Properties props = new Properties();                                                    //ery ye göre ayarlanıcak
            props.put("mail.smtp.host", "smtpout.secureserver.net");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "465");

            // E-posta oturumu oluştur ve kimlik doğrulaması yap
            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            // Excel satırlarını oku ve e-posta gönder
            Map<String, List<String[]>> groupedData = new HashMap<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) { // İlk satırı atla
                XSSFRow row = sheet.getRow(rowIndex);

                // Hücrelerden değerleri al
                XSSFCell nameCell = row.getCell(1);
                XSSFCell Faturano = row.getCell(2);
                XSSFCell Vadetarih = row.getCell(3);
                XSSFCell DolarTutar = row.getCell(4);
                XSSFCell TlTutar = row.getCell(5);
                XSSFCell Mail = row.getCell(6);

                // Gerekli hücrelerin olup olmadığını kontrol et
                if (Faturano == null || Vadetarih == null || DolarTutar == null || Mail == null || TlTutar == null) {
                    throw new MessagingException("Excel file content is incorrect or missing at row, please make sure its right." + (rowIndex + 1));
                }

                // E-posta adresi ve isim gibi verileri al
                String recipientEmail = getCellValueAsString(Mail)
                        ;
                String recipientName = getCellValueAsString(nameCell);
                String FaturaNo = getCellValueAsString(Faturano);
                String dolartutar = getCellValueAsString(DolarTutar);
                String Tltutar = getCellValueAsString(TlTutar);
                double serialDate = Vadetarih.getNumericCellValue();
                Date javaDate = DateUtil.getJavaDate(serialDate);
                DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                String VadeTarih = df.format(javaDate);

                String [] emailAdresses = recipientEmail.split(";");

                for (String emailAdress : emailAdresses) {


                    // E-posta adresine göre veri gruplama
                    if (!groupedData.containsKey(emailAdress)) {
                        groupedData.put(emailAdress, new ArrayList<>());
                    }
                    groupedData.get(emailAdress).add(new String[]{FaturaNo, VadeTarih, dolartutar, recipientName, Tltutar, recipientEmail});
                }






            }

            // Gruplandırılmış verileri kullanarak e-posta gönder
            for (Map.Entry<String, List<String[]>> entry : groupedData.entrySet()) {
                String recipientEmail = entry.getKey();
                List<String[]> invoices = entry.getValue();
                String recipientName = invoices.get(0)[3]; // Bu alıcı için ismi ilk faturadan al

                String emailSubject = "Your email subject";
                StringBuilder emailBody = new StringBuilder("<html>" +
                        "<head>" +
                        "<style>" +
                        "body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                        ".small2{font-size: 0.8em;}"+
                        ".container { width: 100%; max-width: 600px; margin: 20px auto; background-color: #ffffff; border: 1px solid #dddddd; padding: 20px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); }" +
                        ".header { text-align: center; border-bottom: 1px solid #dddddd; padding-bottom: 10px; margin-bottom: 20px; }" +
                        ".header h2 { margin: 0; }" +
                        ".content { line-height: 1.6; }" +
                        ".footer { border-top: 1px solid #dddddd; padding-top: 10px; margin-top: 20px; font-size: 0.9em; color: #555555; display: flex; justify-content: space-between; align-items: flex-start; }" +
                        ".footer img { max-width: 150px; margin-left: 10px; }" +
                        ".footer div { text-align:left; flex-grow: 1; }" +
                        ".footer a { color: #0073e6; text-decoration: none; }" +
                        "table { border-collapse: collapse; width: 100%; }" +
                        "th, td { border: 1px solid #dddddd; padding: 8px; text-align: left; }" +
                        "th { background-color: #f0f0f0; }" +
                        "</style>" +
                        "</head>"+
                        "<body>" +
                        "<div class='container'>" +
                        "<div class='header'>" +
                        "<h2>ERY Bilişim Cari Faturalar Hakkında</h2>" +
                        "</div>" +
                        "<div class='content'>" +
                        "<p>Sayın " + recipientName + ",</p>" +
                        "<p>Bu e-posta, ERY Bilişim'e ait vadesi gelen ödemeniz hakkında hatırlatma amacıyla gönderilmiştir. Ödeme detaylarınız aşağıdadır:</p>" +
                        "<table>" +
                        "<tr>" +
                        "<th>Fatura Numarası</th>" +
                        "<th>Vade Tarihi</th>" +
                        "<th>Dolar Tutar</th>" +
                        "<th>TL Tutar</th>" +
                        "</tr>");

                double totalDolarTutar = 0;
                double totalTlTutar = 0;

                // Her fatura için tabloyu oluştur ve toplamları hesapla
                for (String[] invoice : invoices) {
                    emailBody.append("<tr>")
                            .append("<td>").append(invoice[0]).append("</td>")
                            .append("<td>").append(invoice[1]).append("</td>")
                            .append("<td>").append(invoice[2]).append(" </td>")
                            .append("<td>").append(invoice[4]).append(" </td>")
                            .append("</tr>");

                    totalDolarTutar += Double.parseDouble(invoice[2].replaceAll("[^\\d.]", ""));
                    totalTlTutar += Double.parseDouble(invoice[4].replaceAll("[^\\d.]", ""));
                }

                // Toplamları tabloya ekle
                DecimalFormat df = new DecimalFormat("#.00");

                emailBody.append("<tr><td colspan='2'><b>Toplam Tutar</b></td>")
                        .append("<td><b>").append(df.format(totalDolarTutar)).append(" $</b></td>")
                        .append("<td><b>").append(df.format(totalTlTutar)).append(" ₺</b></td></tr>")
                        .append("</table>" +
                                "<p>Ödemenizi henüz yapmadıysanız,aşağıdaki banka bilgileri ile lütfen ödemenizi en kısa sürede gerçekleştirmenizi rica ederiz.</p>" +
                                "<p>Sorularınız için bizimle iletişime geçebilirsiniz.</p>" +
                                "<p>Teşekkürler,</p>" +
                                "<p><small><i>Tahsilat işlemlerinde 'ÖDEME GÜNÜNDEKİ TCMB EFEKTİF SATIŞ KURU' uygulanmaktadır.</small></p>" +
                                "<p><small><i>Bu faturayı ödediyseniz bu maili dikkate almayınız, eğer bir yanlışlık olduğunu düşünüyorsanız aşağıdaki numaradan iletişime geçiniz."+
                                "</div>"+
                                "<div class='footer'>"+
                                "<div>"+
                                "<p><small><strong> ÖDEMENİN YAPILACAĞI İBAN'LAR</small></p>"+
                                "<hr>"+
                                "<p style=\"font-size: 0.9em;\"><small><strong>VAKIFBANK USD</strong>  TR80 0001 5001 5804 8019 7467 49 <strong>EUR</strong> TR96 0001 5001 5804 8019 7467 52 <strong>TL</strong> TR68 0001 5001 5800 7313 4659 11</small></p>" +
                                "<p style=\"font-size: 0.9em;\"><small><strong>HALKBANK USD</strong>  TR50 0001 2009 4100 0053 0004 79 <strong>EUR</strong> TTR78 0001 2009 4100 0058 0005 19 <strong>TL</strong> TR28 0001 2009 4100 0010 2611 23</small></p>"+
                                "<p style=\"font-size: 0.9em;\"><small><strong>GARANTİ USD</strong>  TR11 0006 2000 6820 0009 0901 84 <strong>EUR</strong> TR38 0006 2000 6820 0009 0901 83 <strong>TL</strong> TR27 0006 2000 6820 0006 2971 69</small></p>"+
                                "<p><strong>Muhasebe ve Finansman Departmanı</strong></p>"+
                                "<p>M: +90 542 301 03 79</p>"+
                                "<p>T: 0312 267 5379"+
                                "<p>E: <a href='mailto:muhasebe@erysystem.com'>muhasebe@erysystem.com</a></p>"+
                                "</div>"+
                                "</div>"+
                                "</div>"+
                                "</body>" +
                                "</html>");

                // E-postayı oluştur ve gönder
                try {
                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress("frombatuhanoguz@batuhanoguz.com"));            //ery ile değişicek
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                    message.setSubject(emailSubject);
                    message.setContent(emailBody.toString(), "text/html; charset=utf-8");

                    Transport.send(message);
                    System.out.println("Email sent successfully to " + recipientEmail);
                } catch (SendFailedException e) {
                    throw new MessagingException("Invalid recipient address: " + recipientEmail, e);
                } catch (AuthenticationFailedException e) {
                    throw new MessagingException("Invalid username or password", e);
                } catch (MessagingException e) {
                    System.err.println("Error sending email to " + recipientEmail);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new MessagingException("Error reading Excel file: " + e.getMessage(), e);
        }
    }

    // Hücre değerlerini uygun formata çeviren yardımcı metod
    private String getCellValueAsString(XSSFCell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                    return df.format(cell.getDateCellValue());
                } else {
                    // Sayısal hücreleri uygun formatta döndür
                    return String.format("%.2f", cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}