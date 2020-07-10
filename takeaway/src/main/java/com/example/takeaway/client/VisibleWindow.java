package com.example.takeaway.client;

import org.springframework.stereotype.Repository;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

/**
 * 创建可视化窗口
 * ctrl+alt+L
 */
@Repository
public class VisibleWindow extends JFrame implements ActionListener {
    private static final long serialVersionUID = 2016913328739206536L;
    // 选择的文件(用户在文件选择器中选择的)
    private List<File> userSelectedFiles = new ArrayList<File>();
    // 我们经过分析得到的最终会被打包的文件
    private List<File> finalFiles = new ArrayList<File>();
    // 文件打包路径及物理文件
    private Map<String, File> filePaths = new Hashtable<>();

    public VisibleWindow() {
        setSize(480, 320);
        setResizable(false);
        setLocationRelativeTo(null);
        setTitle("Jar包生成工具");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(null);
//        setIconImage(Toolkit.getDefaultToolkit().getImage(VisibleWindow.class.getResource("/resources/icon.png")));
        initComponents();
    }

    private JLabel lblTip;
    private JButton btnBrowser;
    private JFileChooser jfcSelect;
    private JTextArea txtFiles;
    private JComboBox<String> cobMainClass;
    private JButton btnCls;
    private JButton btnConfirm;
    private JFileChooser jfcSave;

    // 初始化组件
    private void initComponents() {
        // 提示
        lblTip = new JLabel("请选择需要打包的文件！");
        lblTip.setLocation(20, 10);
        lblTip.setSize(350, 20);
        add(lblTip);

        // 浏览按钮
        btnBrowser = new JButton("浏 览");
        btnBrowser.setLocation(380, 10);
        btnBrowser.setSize(80, 24);
        btnBrowser.addActionListener(this);
        add(btnBrowser);

        // 展示已选择文件
        JScrollPane jspFiles = new JScrollPane();
        txtFiles = new JTextArea();
        txtFiles.setEditable(false);
        jspFiles.setSize(440, 160);
        jspFiles.setLocation(20, 40);
        txtFiles.setSize(440, 201600);
        txtFiles.setLocation(20, 40);
        txtFiles.setFocusable(false);
        jspFiles.setViewportView(txtFiles);
        add(jspFiles);

        // 选择启动类
        cobMainClass = new JComboBox<>();
        cobMainClass.setSize(440, 30);
        cobMainClass.setLocation(20, 210);
        add(cobMainClass);

        // 清除已选
        btnCls = new JButton("重 选");
        btnCls.setLocation(20, 250);
        btnCls.setSize(80, 24);
        btnCls.addActionListener(this);
        add(btnCls);

        // 确认按钮
        btnConfirm = new JButton("确认");
        btnConfirm.setSize(80, 24);
        btnConfirm.setLocation(380, 250);
        btnConfirm.addActionListener(this);
        add(btnConfirm);

        // 文件选择器
        jfcSelect = new JFileChooser();
        // 可以选择文件和文件夹
        jfcSelect.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        // 可以多选
        jfcSelect.setMultiSelectionEnabled(true);

        // 文件保存
        jfcSave = new JFileChooser();
        // 设置只接受以“.jar”结尾的文件
        jfcSave.setAcceptAllFileFilterUsed(false);
        jfcSave.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "可执行Jar";
            }

            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".jar");
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnBrowser) {
            // 浏览
            int result = jfcSelect.showOpenDialog(this);

            // 选择了文件
            if (result == JFileChooser.APPROVE_OPTION) {
                for (File file : jfcSelect.getSelectedFiles())
                    userSelectedFiles.add(file);

                // 整理选择的文件，去除重复项
                removeDuplicateItems(userSelectedFiles);

                // 重新计算选中文件
                finalFiles.clear();
                for (File file : userSelectedFiles)
                    addFileToList(file, finalFiles);

                // 计算文件展示打包路径及展示路径
                // 计算可启动类路径
                // 展示到文本框中
                cobMainClass.removeAllItems();
                txtFiles.setText("");
                File file, direc;
                String filePath, direcPath;
                Iterator<File> itd, itf;
                for (itd = userSelectedFiles.iterator(); itd.hasNext(); ) {
                    direc = itd.next();
                    direcPath = direc.getAbsolutePath();
                    for (itf = finalFiles.iterator(); itf.hasNext(); ) {
                        file = itf.next();
                        filePath = file.getAbsolutePath();
                        if (filePath.equalsIgnoreCase(direcPath)) {
                            txtFiles.append(file.getName() + "\n");
                            filePaths.put(file.getName(), file);
                            if (file.getName().endsWith(".class"))
                                cobMainClass.addItem(file.getName().endsWith(".class") ? file.getName().substring(0, file.getName().lastIndexOf('.')) : file.getName());
                            itf.remove();
                        } else if (filePath.startsWith(direcPath)) {
                            String nameTmp = filePath.substring(direcPath.lastIndexOf(File.separator) + 1).replace(File.separatorChar, '/');
                            filePaths.put(nameTmp, file);
                            txtFiles.append(nameTmp + "\n");
                            if (nameTmp.endsWith(".class") && nameTmp.indexOf('$') == -1)
                                cobMainClass.addItem(nameTmp.substring(0, nameTmp.lastIndexOf('.')).replace('/', '.'));
                            itf.remove();
                        }
                    }
                }
            }
        } else if (e.getSource() == btnCls) {
            if (userSelectedFiles.size() == 0) return;
            else if (JOptionPane.showConfirmDialog(this, "确定重选吗？将清除所有已选项！") == JOptionPane.OK_OPTION) {
                userSelectedFiles.clear();
                finalFiles.clear();
                filePaths.clear();
                cobMainClass.removeAllItems();
            }
        } else if (e.getSource() == btnConfirm) {
            if (filePaths.size() == 0) {
                JOptionPane.showMessageDialog(this, "未选择文件", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (cobMainClass.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "未选择启动类", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            //增加判断是否有人正在打包
                //如果有人打包，则返回有人正在打包，请等待
                    //加锁

                //打包成功后我添加到表里做记录


                //http加请求

            // 打包
            int result = jfcSave.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    // 清单文件
                    Manifest man = new Manifest();
                    // 版本和启动类路径必要
                    man.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
                    man.getMainAttributes().putValue(Attributes.Name.MAIN_CLASS.toString(), cobMainClass.getSelectedItem().toString());
                    // Class-Path一定不要，除非能保证将引用类(即import的类)都联合打包了
                    JarOutputStream jos = new JarOutputStream(new FileOutputStream(jfcSave.getSelectedFile()), man);
                    jos.setLevel(Deflater.BEST_COMPRESSION);
                    BufferedInputStream bis = null;
                    byte[] cache = new byte[1024];
                    StringBuffer config = new StringBuffer();
                    for (String name : filePaths.keySet()) {
                        bis = new BufferedInputStream(new FileInputStream(filePaths.get(name)), 1024);
                        config.append(name).append('=').append(bis.available()).append('\n');
                        jos.putNextEntry(new JarEntry(name));
                        int count;
                        while ((count = bis.read(cache, 0, 1024)) != -1)
                            jos.write(cache, 0, count);
                        jos.closeEntry();
                        bis.close();
                    }
                    jos.flush();
                    jos.close();
                    JOptionPane.showMessageDialog(this, "导出成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "异常", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

    }

    // 添加文件(非文件夹)到集合
    private void addFileToList(File file, List<File> fileArr) {
        if (file.isDirectory())
            for (File child : file.listFiles())
                addFileToList(child, fileArr);
        else
            fileArr.add(file);
    }

    // 去除重复项
    private void removeDuplicateItems(List<File> fileArr) {
        // 去重复项
        Set<String> directories = new HashSet<>();
        Set<String> files = new HashSet<>();
        for (File file : fileArr)
            if (file.isDirectory())
                directories.add(file.getAbsolutePath());
            else
                files.add(file.getAbsolutePath());
        //去包含项(先去文件夹再去文件应该更好)
        String fpath, dpath;
        for (Iterator<String> itf = files.iterator(); itf.hasNext(); ) {
            fpath = itf.next();
            for (Iterator<String> itd = directories.iterator(); itd.hasNext(); ) {
                dpath = itd.next();
                if (fpath.startsWith(dpath))
                    itf.remove();
            }
        }
        String dpath1, dpath2;
        Set<String> directories1 = new HashSet<>(directories);
        for (Iterator<String> itd1 = directories.iterator(); itd1.hasNext(); ) {
            dpath1 = itd1.next();
            for (Iterator<String> itd2 = directories1.iterator(); itd2.hasNext(); ) {
                dpath2 = itd2.next();
                if (dpath1.equals(dpath2))
                    continue;
                else if (dpath2.startsWith(dpath1))
                    itd2.remove();
                else if (dpath1.startsWith(dpath2))
                    itd1.remove();
            }
        }
        directories.addAll(directories1);

        fileArr.clear();
        for (String file : files)
            fileArr.add(new File(file));
        for (String directory : directories)
            fileArr.add(new File(directory));
    }

}


