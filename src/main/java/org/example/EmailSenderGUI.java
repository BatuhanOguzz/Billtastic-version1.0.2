package org.example;

import com.toedter.calendar.JDateChooser;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Timer;
import java.util.Calendar;

public class EmailSenderGUI {
    private JFrame frame;
    private JTextField excelFileTextField;
    private JTextField usernameTextField;
    private JPasswordField passwordPasswordField;
    private JTextArea terminalTextArea;
    private JDateChooser dateChooser;
    private JSpinner timeSpinner;
    private DefaultListModel<Date> dateListModel;
    private JList<Date> dateList;
    private Set<Date> sentDates = new HashSet<>(); // Gönderilen tarihlerin takibi
    private JTable dateTable;
    private Set<Date> unsentDates = new HashSet<>(); // Gönderilmeyen tarihlerin takibi
    private Map<Date, Timer>scheduledTasks = new HashMap<>();

    public EmailSenderGUI() {
        createGUI();
        scheduleServerConnectionLogging(); // Sunucu bağlantı durumunu planla
        startDateCheckTimer(); // Tarihleri kontrol etmek için zamanlayıcı başlat
    }

    private void createGUI() {
        frame = new JFrame("ERY System-Billtastic");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        frame.setPreferredSize(new Dimension(500, 600));
        frame.setResizable(false);


        // Uygulama simgesini ayarla
        ImageIcon icon = new ImageIcon("C:\\Users\\batuh\\Desktop\\Deneme2\\icon.png");
        Image image = icon.getImage();
        Image newimg = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        icon = new ImageIcon(newimg);
        frame.setIconImage(icon.getImage());

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Excel dosyası girişi
        excelFileTextField = new JTextField();
        JLabel excelFileLabel = new JLabel("Excel Dosyası:", SwingConstants.RIGHT);
        JButton browseButton = getBrowseButton();

        gbc.gridx = 0;
        gbc.gridy = 1;
        frame.add(excelFileLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        frame.add(excelFileTextField, gbc);

        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        frame.add(browseButton, gbc);

        // Kullanıcı adı girişi
        usernameTextField = new JTextField();
        JLabel usernameLabel = new JLabel("E-Posta Adresi:", SwingConstants.RIGHT);

        gbc.gridx = 0;
        gbc.gridy = 2;
        frame.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        frame.add(usernameTextField, gbc);

        // Şifre girişi
        passwordPasswordField = new JPasswordField();
        JLabel passwordLabel = new JLabel("Şifre:", SwingConstants.RIGHT);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        frame.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        frame.add(passwordPasswordField, gbc);

        // Tarih seçici ve saat seçici
        dateChooser = new JDateChooser();
        dateChooser.setMinSelectableDate(new Date()); // Minimum seçilebilir tarihi bugüne ayarla
        JLabel dateLabel = new JLabel("Tarih seçiniz:", SwingConstants.LEFT);

        timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm:ss");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setValue(new Date()); // Varsayılan olarak mevcut saati ayarla

        JButton addDateButton = new JButton("Tarih ekle");
        addDateButton.addActionListener(e -> {
            Date selectedDate = dateChooser.getDate();
            Date selectedTime = (Date) timeSpinner.getValue();
            if (selectedDate != null && selectedTime != null) {
                Calendar selectedDateTime = Calendar.getInstance();
                selectedDateTime.setTime(selectedDate);
                Calendar selectedTimeCalendar = Calendar.getInstance();
                selectedTimeCalendar.setTime(selectedTime);

                selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedTimeCalendar.get(Calendar.HOUR_OF_DAY));
                selectedDateTime.set(Calendar.MINUTE, selectedTimeCalendar.get(Calendar.MINUTE));
                selectedDateTime.set(Calendar.SECOND, selectedTimeCalendar.get(Calendar.SECOND));

                Date combinedDateTime = selectedDateTime.getTime();
                if (combinedDateTime.before(new Date())) {
                    appendToTerminal(getCurrentDateTime() + " Seçtiğiniz tarih geçmiş tarih olamaz, lütfen yeni bir tarih seçiniz.");
                    return;
                }

                // Türkçe tarih formatı
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, HH:mm:ss yyyy", new Locale("tr", "TR"));
                String formattedDate = dateFormat.format(combinedDateTime);

                // JTable'a tarih eklemek için DefaultTableModel kullanıyoruz
                DefaultTableModel tableModel = (DefaultTableModel) dateTable.getModel();
                tableModel.addRow(new Object[]{combinedDateTime,"   Planlanmadı",false});
                unsentDates.add(combinedDateTime); // Gönderilmeyen tarihler listesine ekle
                appendToTerminal(getCurrentDateTime() + " Tarih eklendi: " + formattedDate);
            } else {
                appendToTerminal(getCurrentDateTime() + " Tarih veya saat seçilmedi.");
            }
        });


// JTable oluşturuyoruz
        String[] columns = {"Tarih", "Durum",""};

        dateTable = new JTable(new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return String.class;
                }
                if (columnIndex == 0) {
                    return Date.class; // Tarih sütunu için Date sınıfı kullanıyoruz
                }
                if (columnIndex == 2) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == 1) {
                    return false;
                }
                return false;
            }
        });

        JCheckBox headerCheckBox = new JCheckBox("");
        headerCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Tümünü seç veya tümünü kaldır
                for (int i = 0; i < dateTable.getRowCount(); i++) {
                    dateTable.setValueAt(headerCheckBox.isSelected(), i, 2);
                }
            }
        });

        JTableHeader header = dateTable.getTableHeader();
        header.getColumnModel().getColumn(2).setHeaderRenderer(new org.example.CheckBoxHeaderRenderer(headerCheckBox));
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                    int columnIndex = header.columnAtPoint(e.getPoint());
                    if (columnIndex == 2) {
                        headerCheckBox.doClick();
                    }
                }
            }
        });


// Tablonun hücrelerindeki tarihlerin arka plan renklerini değiştirmek için özel bir render kullanıyoruz
        dateTable.getColumnModel().getColumn(0).setCellRenderer(new org.example.DateTableCellRenderer(sentDates, unsentDates));

// Tümünü seç checkboxı ekliyoruz
        JCheckBox selectAllCheckBox = new JCheckBox();
        selectAllCheckBox.addActionListener(e -> {
            boolean isSelected = selectAllCheckBox.isSelected();
            DefaultTableModel tableModel = (DefaultTableModel) dateTable.getModel();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(isSelected, i, 2);
            }
        });

        dateTable.getColumnModel().getColumn(0).setMaxWidth(320); // CheckBox sütun genişliğini ayarlıyoruz
        dateTable.getColumnModel().getColumn(0).setMinWidth(320);
        dateTable.getColumnModel().getColumn(1).setMaxWidth(90);
        dateTable.getColumnModel().getColumn(1).setMinWidth(90);

        JScrollPane dateScrollPane = new JScrollPane(dateTable);
        dateScrollPane.setPreferredSize(new Dimension(150, 150));


        // Tarih silme butonu
        JButton removeDateButton = new JButton("Tarih sil");
        removeDateButton.addActionListener(e -> removeSelectedDates());

        // Gönderme butonu
        JButton sendButton = new JButton("E-postaları Planla");
        sendButton.addActionListener(e -> {
            int rowCount = dateTable.getModel().getRowCount();
            for (int i = 0; i < rowCount; i++) {
                Boolean isChecked = (Boolean) dateTable.getModel().getValueAt(i, 2);
                if (isChecked) {
                    Date date = (Date) dateTable.getModel().getValueAt(i, 0);
                    String excelFile = excelFileTextField.getText();
                    String username = usernameTextField.getText();
                    String password = new String(passwordPasswordField.getPassword());

                    // "Durum" sütununu "Planlandı" olarak güncelle
                    dateTable.getModel().setValueAt("   Planlandı", i, 1);

                    // E-postayı planla
                    scheduleEmailSending(date, excelFile, username, password);
                    appendToTerminal(getCurrentDateTime() + " E-postalar bu tarihe ayarlandı: " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(date));
                }
            }
        });

        // Anında gönderme butonu
        JButton forceSendButton = new JButton("Anında Ekstre Gönderme");
        forceSendButton.addActionListener(e -> {
            Object[] options = {"Evet", "Hayır"};
            int response = JOptionPane.showOptionDialog(frame, "\"Anında Ekstre Gönderme\" seçeneğini kullanmak istediğinizden emin misiniz?", "Onay", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (response == 0) {
                String excelFile = excelFileTextField.getText();
                String username = usernameTextField.getText();
                String password = new String(passwordPasswordField.getPassword());
                sendEmailsImmediately(excelFile, username, password);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 4;
        frame.add(dateLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        frame.add(dateChooser, gbc);

        gbc.gridx = 2;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        frame.add(timeSpinner, gbc);

        gbc.gridx = 3;
        gbc.gridy = 4;
        frame.add(addDateButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        frame.add(dateScrollPane, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        frame.add(removeDateButton, gbc); // Tarih silme butonunu ekle

        gbc.gridx = 2;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        frame.add(sendButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 4;
        frame.add(forceSendButton, gbc); // Anında gönderme butonunu ekle

        terminalTextArea = new JTextArea(10, 40);
        terminalTextArea.setEditable(false);
        JScrollPane terminalScrollPane = new JScrollPane(terminalTextArea);
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 4;
        frame.add(terminalScrollPane, gbc);

        frame.pack();
        frame.setVisible(true);
    }

    private void appendToTerminal(String message) {
        terminalTextArea.append(message + "\n");
    }

    private String getCurrentDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("tr", "TR"));
        return dateFormat.format(new Date());
    }

    private JButton getBrowseButton() {
        JButton browseButton = new JButton("Gözat");
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                excelFileTextField.setText(selectedFile.getAbsolutePath());
            }
        });
        return browseButton;
    }

    private void scheduleEmailSending(Date date, String excelFile, String username, String password) {
        Timer timer = new Timer();
        scheduledTasks.put(date, timer); // Planlanan görevi kaydet

        timer.schedule(new TimerTask() {
            public void run() {
                File file = new File(excelFile);
                if (!file.exists() || !file.canRead()) {
                    SwingUtilities.invokeLater(() -> {
                        terminalTextArea.append("Hata: Excel dosyası bulunamıyor veya okunamıyor: " + excelFile + "\n");
                    });
                    return;
                }
                EmailSender emailSender = new EmailSender();
                try {
                    emailSender.sendEmails(excelFile, username, password);
                    sentDates.add(date); // Gönderilen tarihler listesine ekle
                    unsentDates.remove(date); // Gönderilmeyen tarihler listesinde kaldır
                    SwingUtilities.invokeLater(() -> {
                        appendToTerminal(getCurrentDateTime() + " E-postalar başarıyla gönderildi.");
                        dateTable.repaint(); // Tablonun yeniden boyanmasını sağlayın
                    });
                } catch (MessagingException e) {
                    SwingUtilities.invokeLater(() -> {
                        if (e.getCause() instanceof AuthenticationFailedException) {
                            terminalTextArea.append("Hata: Geçersiz SMTP bilgisi\n");
                        } else {
                            terminalTextArea.append("Bir hata oluştu: " + e.getMessage() + "\n");
                        }
                    });
                }
            }
        }, date);
    }

    private void sendEmailsImmediately(String excelFile, String username, String password) {
        File file = new File(excelFile);
        if (!file.exists() || !file.canRead()) {
            SwingUtilities.invokeLater(() -> {
                terminalTextArea.append("Hata: Excel dosyası bulunamıyor veya okunamıyor: " + excelFile + "\n");
            });
            return;
        }
        EmailSender emailSender = new EmailSender();
        try {
            emailSender.sendEmails(excelFile, username, password);
            SwingUtilities.invokeLater(() -> {
                appendToTerminal(getCurrentDateTime() + " E-postalar başarıyla gönderildi.");
                dateTable.repaint(); // Tablonun yeniden boyanmasını sağlayın
            });
        } catch (MessagingException e) {
            SwingUtilities.invokeLater(() -> {
                if (e.getCause() instanceof AuthenticationFailedException) {
                    terminalTextArea.append("Geçersiz SMTP bilgisi\n");
                } else {
                    terminalTextArea.append("Bir hata oluştu: " + e.getMessage() + "\n");
                }
            });
        }
    }


    private void removeSelectedDates() {
        DefaultTableModel tableModel = (DefaultTableModel) dateTable.getModel();

        // Seçili satırları sil
        for (int i = tableModel.getRowCount() - 1; i >= 0; i--) {
            Boolean isChecked = (Boolean) tableModel.getValueAt(i, 2);
            if (isChecked) {
                Date date = (Date) tableModel.getValueAt(i, 0);
                unsentDates.remove(date); // Gönderilmeyen tarihler listesinde kaldır
                tableModel.removeRow(i);

                // Planlanan görevi iptal et
                Timer timer = scheduledTasks.get(date);
                if (timer != null) {
                    timer.cancel();
                    scheduledTasks.remove(date);
                }
            }
        }
        appendToTerminal(getCurrentDateTime() + " Seçili tarihler silindi.");
    }


    private void scheduleServerConnectionLogging() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("u2soft.com", 443); // SMTP sunucu adresini ve portunu buraya koyun
                    appendToTerminal(getCurrentDateTime() + " Sunucuya bağlantı başarılı.");
                    socket.close();
                } catch (Exception e) {
                    appendToTerminal(getCurrentDateTime() + " Sunucuya bağlantı hatası: " + e.getMessage());
                }
            }
        };
        timer.scheduleAtFixedRate(task, 0, 600000); // Her dakika bağlantıyı kontrol et
    }

    private void startDateCheckTimer() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Date now = new Date();
                for (Date date : new HashSet<>(unsentDates)) { // Set kopyası oluşturulur, çünkü doğrudan silmek iterasyonu etkiler
                    if (date.before(now)) {
                        scheduleEmailSending(date, excelFileTextField.getText(), usernameTextField.getText(), new String(passwordPasswordField.getPassword()));
                        unsentDates.remove(date);
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(task, 0, 60000); // Her dakika tarihi kontrol et
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(EmailSenderGUI::new);
    }
}