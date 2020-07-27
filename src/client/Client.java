package client;

import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

public class Client {
	private JFrame frame;
	private JTextArea viewArea;
	private JTextField viewField;
	private JButton send;
	private JButton siliao;
	private JButton qunliao;
	private JLabel state; // 聊天的状态
	private JButton exit;
	private JButton clear;
	private JButton file;
	public String[] friends;
	private String userName = "";
	private Socket socket;
	private PrintStream ps;
	private BufferedReader br;

	private JFileChooser jfc;
	private File fi;
	private BufferedReader filebr;
	private boolean flag = true;
	String name = "";

	public void init() {
		try {
			// 连接到服务器
			socket = new Socket("127.0.0.1", 4700);
			// 获取该Socket对应的输入流和输出流
			ps = new PrintStream(socket.getOutputStream());
			br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			String input = "";

			// 输入用户名不满足条件时不断地弹出对话框要求重新输入
			while (true) {
				userName = JOptionPane.showInputDialog(input + "输入用户名");
				if (userName == null) {
					return;
				}
				// 将用户输入的用户名的前后增加协议字符串后发送
				ps.println(MyString.USER_ROUND + userName + MyString.USER_ROUND);
				String str = br.readLine();
				if (br.equals(MyString.NAME_ERROR)) {
					input = "不合格";
					continue;
				}
				// 如果服务器返回登录成功，结束循环
				if (str.equals(MyString.LOGIN_SUCCESS)) {
					break;
				}
			}
			initFrame(); // 创建窗口
		}
		// 捕捉到异常，关闭网络资源，并退出该程序
		catch (Exception e) {
			ps.close();
			System.exit(1);
		}
		// 以该Socket对应的输入流启动ClientThread线程
		new ClientThread(br).start();
	}

	class MyListener implements ActionListener {
		String message = null;

		public void actionPerformed(ActionEvent e) {
			String message = viewField.getText();
			switch (e.getActionCommand()) {
			case "发送":
				if (message.length() == 0) { // 禁止发空消息

				} else if (flag) {
					ps.println(MyString.MSG_ROUND + message
							+ MyString.MSG_ROUND);// 群聊，所有用户都能收到信息
				} else { // 私聊
					ps.println(MyString.PRIVATE_ROUND + name
							+ MyString.SPLIT_SIGN + message
							+ MyString.PRIVATE_ROUND);
				}
				viewField.setText("");
				viewField.requestFocus();
				break;
			case "清屏":
				viewArea.setText("");
				break;
			case "退出":
				ps.println(MyString.EXIT + MyString.EXIT);// 发送退出信息
				System.exit(0);
				break;
			case "私聊":
				name = JOptionPane.showInputDialog("输入对方的名字");
				while (name == null) {
					name = JOptionPane.showInputDialog("输入对方的名字");
				}

				ps.println(MyString.PRIVATE_ROUND + name + MyString.SPLIT_SIGN
						+ MyString.PRIVATE_ROUND);
				flag = false;
				if (name.length() > 0 && name.length() < 5) {
					state.setText("与" + name + "\n私聊中");
				}

				else
					state.setText("与*****私聊中"); // 字数过长，限制
				break;
			case "群聊":
				flag = true;
				state.setText("群聊中");
				break;
			}
		}
	}

	public static void main(String[] args) {
		new Client().init();
	}

	public void initFrame() {
		frame = new JFrame(userName + "的聊天室");
		frame.setSize(20, 50);
		viewArea = new JTextArea(10, 40);
		viewField = new JTextField(50);
		send = new JButton("发送");
		send.setMnemonic(KeyEvent.VK_ENTER);
		file = new JButton("文件发送");
		file.setSize(20, 30);
		siliao = new JButton("私聊");
		qunliao = new JButton("群聊");
		state = new JLabel("群聊中");
		exit = new JButton("退出");
		clear = new JButton("清屏");
		JPanel panel = new JPanel();
		panel.add(viewField);
		panel.add(send);
		Box liao = Box.createHorizontalBox();
		liao.add(siliao);
		liao.add(qunliao);
		Box clearExit = Box.createHorizontalBox();
		clearExit.add(clear);
		clearExit.add(exit);
		JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayout(4, 1, 4, 4));
		panel2.add(state);
		panel2.add(liao);
		panel2.add(file);
		panel2.add(clearExit);
		JScrollPane sp = new JScrollPane(viewArea);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.add(sp);
		frame.add(panel2, BorderLayout.EAST);
		frame.add(panel, BorderLayout.SOUTH);
		frame.setSize(500, 250);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				ps.println(MyString.EXIT); // 退出
				if (ps != null)
					ps.close();
				System.exit(0);
			}
		});
		frame.pack();
		frame.setVisible(true);
		viewField.requestFocus(); // 设置焦点
		MyListener ml = new MyListener();
		send.addActionListener(ml);
		exit.addActionListener(ml);
		clear.addActionListener(ml);
		siliao.addActionListener(ml);
		qunliao.addActionListener(ml);
		file.addActionListener(new MyFileListener());
	}

	class MyFileListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int reval = jfc.showOpenDialog(frame);
			if (reval == JFileChooser.APPROVE_OPTION) {
				fi = jfc.getSelectedFile(); // 选择文件

			} else if (reval == 1) {
				return;
			} else {
				JOptionPane.showMessageDialog(frame, "操作有误请稍后再试！！");
				return;
			}

			try {
				filebr = new BufferedReader(new InputStreamReader(
						new FileInputStream(fi), "UTF-8"));
				String filename = fi.getName();
				if (!flag) { // 正在私聊，文件名发送给个人
					ps.println(MyString.PRIVATE_ROUND + name
							+ MyString.SPLIT_SIGN + MyString.PRIVATE_ROUND);
					viewArea.append("文件" + filename + "向" + name
							+ "发送中.......\n");
					ps.println(MyString.FILE_NAME + MyString.PRIVATE_FILE
							+ name + MyString.SPLIT_SIGN + filename
							+ MyString.FILE_NAME); // 首先发送文件名
				} else {
					viewArea.append("文件" + filename + "向所有人发送中.......\n");
					ps.println(MyString.FILE_NAME + filename
							+ MyString.FILE_NAME);
				}
				String read;
				while ((read = filebr.readLine()) != null) { // 一行一行向服务器发送
					ps.println(MyString.FILE_ROUND + read + MyString.FILE_ROUND);
				}
				ps.println(MyString.FILE_END); // 文件发送完毕
			} catch (Exception e1) {
			}
			viewArea.append("文件发送完毕.....,已存入项目目录下\n");
		}
	}

	class ClientThread extends Thread {
		private BufferedReader br = null;
		private OutputStreamWriter out; // 用于接收文件
		private String fileName;

		public ClientThread(BufferedReader br) {
			this.br = br;
		}

		// 将读到的内容去掉前后的特殊字符，得到真实信息
		private String getMessage(String line) {
			return line.substring(2, line.length() - 2);
		}

		public void run() {
			try {
				String line = null;
				// 不断从输入流中读取数据，对数据进行分类处理
				while ((line = br.readLine()) != null) {
					// 获取文件名
					if (line.startsWith(MyString.FILE_NAME)
							&& line.endsWith(MyString.FILE_NAME)) {
						fileName = getMessage(line);

						out = new OutputStreamWriter(new FileOutputStream(
								fileName), "UTF-8");
					}
					// 接收文件
					else if (line.startsWith(MyString.FILE_ROUND)
							&& line.endsWith(MyString.FILE_ROUND)) {
						String userMessage = getMessage(line);
						out.write(userMessage + "\n");
					} else if (line.startsWith(MyString.IS_FLAG)
							&& line.endsWith(MyString.IS_FLAG)) { // 私聊对象不存在时
						flag = true;
						state.setText("群聊中"); // 显示群聊
						viewArea.append(getMessage(line) + "\n");
					}
					// 普通聊天信息时
					else {
						viewArea.append(line + "\n"); // 输出信息到界面上
					}

				}
			} catch (IOException ex) {
			}
			// 使用finally块来关闭该线程对应的输入流
			finally {
				try {
					if (br != null)
						br.close();
					if (out != null)
						out.close();
				} catch (IOException ex) {
				}
			}
		}
	}

}
