package server;

import java.net.*;
import java.text.SimpleDateFormat;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import client.MyString;

public class Server {
	private ServerSocket serverSocket;
	private JFrame frame;
	private JTextArea viewArea;
	private JTextField viewField;
	private JButton send;
	private JList<String> userList;
	private DefaultListModel<String> listModel;
	private SimpleDateFormat sdf;
	// 创建容器存储所有用户
	private static MyMap<String, PrintStream> clients = new MyMap<>();

	public void init() {
		frame = new JFrame("服务器");
		viewArea = new JTextArea(20, 30);
		listModel = new DefaultListModel<String>();
		userList = new JList<String>(listModel);
		viewField = new JTextField(42);
		send = new JButton("发送");
		send.setMnemonic(KeyEvent.VK_ENTER);
		JScrollPane panel = new JScrollPane(userList);
		panel.setBorder(new TitledBorder("在线用户"));
		listModel.addElement("_____________________");
		JScrollPane panel2 = new JScrollPane(viewArea);
		panel2.setBorder(new TitledBorder("用户聊天消息"));
		JPanel panel3 = new JPanel();
		panel3.add(viewField);
		panel3.add(send);
		frame.add(panel, BorderLayout.WEST);
		frame.add(panel2);
		frame.add(panel3, BorderLayout.SOUTH);
		send.addActionListener(e -> {
			String message = viewField.getText();
			if (message.length() == 0) { // 禁止发空消息

			} else {
				for (PrintStream clientPs : clients.valueSet()) {
					clientPs.println("服务器：" + message);
				}
				viewArea.append(sdf.format(System.currentTimeMillis()) + "\t"
						+ "服务器:" + message + "\n");
			}
			viewField.setText("");
			viewField.requestFocus();
		});
		frame.setSize(600, 400);
		frame.pack();
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (serverSocket != null) {
					try {
						serverSocket.close();// 关闭服务器
					} catch (IOException e1) {
					}
				}
				System.exit(0);
			}
		});
		viewField.requestFocus(); // 设置焦点
		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置显示时间的格式
		try {
			// 创建一个ServerSocket在端口4700监听客户请求
			serverSocket = new ServerSocket(4700);
			while (true) { // 循环监听
				// 监听到客户请求，根据得到的Socket对象创建服务线程并启动
				Socket socket = serverSocket.accept();
				new ServerThread(socket).start();
			}

		} catch (IOException ex) {
		}
	}

	public static void main(String[] args) {
		new Server().init();
	}

	class ServerThread extends Thread {
		private Socket socket;
		private BufferedReader br = null;
		private PrintStream ps = null;
		private OutputStreamWriter out = null; // 用于接收文件
		private String fileName;
		private String fileUser;
		private boolean flag = true;

		public ServerThread(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				// 获取该Socket对应的输入流
				br = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));

				// 获取该Socket对应的输出流
				ps = new PrintStream(socket.getOutputStream());
				String line = null;
				while ((line = br.readLine()) != null) {
					// 用户登录的用户名时
					if (line.startsWith(MyString.USER_ROUND)
							&& line.endsWith(MyString.USER_ROUND)) {
						// 得到真实消息
						String userName = getMessage(line);
						if (clients.map.containsKey(userName)
								|| userName.length() == 0
								|| userName.length() > 10) {
							ps.println(MyString.NAME_ERROR); // 限制名字不能为空，不能重复，不能大于十个字符
						} else {
							ps.println(MyString.LOGIN_SUCCESS);
							clients.put(userName, ps);
							listModel.addElement(userName);// 更新在线列表
							viewArea.append(sdf.format(System
									.currentTimeMillis())
									+ "\t"
									+ userName
									+ "上线\n");
						}
					}
					// 私聊信息
					else if (line.startsWith(MyString.PRIVATE_ROUND)
							&& line.endsWith(MyString.PRIVATE_ROUND)) {
						String userMessage = getMessage(line);
						if (userMessage.length() == 1) // 没有任何信息时
							ps.println(MyString.IS_FLAG + "没有任何输入，请输入一个用户名"
									+ MyString.IS_FLAG);
						else {
							String user = userMessage
									.split(MyString.SPLIT_SIGN)[0];// 分割得到名字
							if (!clients.map.containsKey(user)) {
								ps.println(MyString.IS_FLAG
										+ "该用户不存在，请重新输入或者选择群聊！"
										+ MyString.IS_FLAG);
							} else {
								if (userMessage.split(MyString.SPLIT_SIGN).length == 2)// 是否存在信息
								{
									String msg = userMessage
											.split(MyString.SPLIT_SIGN)[1];// 分割得到信息
									// 获取私聊用户对应的输出流发送私聊信息
									clients.map.get(user).println(
											clients.getKeyByValue(ps)
													+ "悄悄地对你说：" + msg);
									ps.println("你悄悄对" + user + "说:" + msg);
									viewArea.append(sdf.format(System
											.currentTimeMillis())
											+ "\t"
											+ clients.getKeyByValue(ps)
											+ "悄悄地对" + user + "说：" + msg + "\n");// 服务器打印聊天信息
								}

							}
						}

					}
					// 获取文件名
					else if (line.startsWith(MyString.FILE_NAME)
							&& line.endsWith(MyString.FILE_NAME)) {
						if (getMessage(line).startsWith(MyString.PRIVATE_FILE)) {
							String message = getMessage(line).substring(1,
									getMessage(line).length());
							fileUser = message.split(MyString.SPLIT_SIGN)[0];
							fileName = message.split(MyString.SPLIT_SIGN)[1];
							clients.map.get(fileUser).println(
									MyString.FILE_NAME + 0 + fileName
											+ MyString.FILE_NAME); // 发送文件名
							flag = false; // 设置为正在私聊
						} else {
							int i = 0;
							fileName = getMessage(line);
							for (PrintStream clientPs : clients.valueSet()) {
								if (clientPs != ps) {// 向除自己之外的所有用户发送文件名
									clientPs.println(MyString.FILE_NAME + i
											+ fileName + MyString.FILE_NAME);
									i++; // 区别文件名
								}

							}
							flag = true;
						}

						out = new OutputStreamWriter(new FileOutputStream(
								fileName), "UTF-8");

					}
					// 接收文件并转发
					else if (line.startsWith(MyString.FILE_ROUND)
							&& line.endsWith(MyString.FILE_ROUND)) {
						String userMessage = getMessage(line);
						if (!flag) {
							clients.map.get(fileUser).println(
									MyString.FILE_ROUND + userMessage
											+ MyString.FILE_ROUND);
						} else {
							for (PrintStream clientPs : clients.valueSet()) {
								if (clientPs != ps) {// 向除自己之外的所有用户发送文件
									clientPs.println(MyString.FILE_ROUND
											+ userMessage + MyString.FILE_ROUND);
								}

							}
						}
						out.write(userMessage + "\n");// 服务器接收文件
					}
					// 成功接收文件时提示已经接收完毕
					else if (line.startsWith(MyString.FILE_END)) {
						viewArea.append(sdf.format(System.currentTimeMillis())
								+ "\t" + "成功接收" + clients.getKeyByValue(ps)
								+ "发送的文件:" + fileName + "\n");
						if (!flag) {
							clients.map.get(fileUser).println(
									"成功接收" + clients.getKeyByValue(ps)
											+ "发送的文件:" + fileName);
						} else {
							for (PrintStream clientPs : clients.valueSet()) {
								if (clientPs != ps) {// 向除自己之外的所有用户发送文件
									clientPs.println("成功接收"
											+ clients.getKeyByValue(ps)
											+ "发送的文件:" + fileName);
								}

							}
						}

					}
					// 用户退出
					else if (line.startsWith(MyString.EXIT)) {
						viewArea.append(sdf.format(System.currentTimeMillis())
								+ "\t" + clients.getKeyByValue(ps) + "下线\n");
						listModel.removeElement(clients.getKeyByValue(ps)); // 移除下线的用户
						clients.removeByValue(ps);
						ps.close();

					}
					// 群聊
					else {
						String msg = getMessage(line);
						// 遍历clients中的每个输出流发送信息
						for (PrintStream clientPs : clients.valueSet()) {
							if (clientPs == ps) {
								ps.println("你说:" + msg);
							} else {
								clientPs.println(clients.getKeyByValue(ps)
										+ "说：" + msg);
							}

						}
						viewArea.append(sdf.format(System.currentTimeMillis())
								+ "\t" + clients.getKeyByValue(ps) + "说：" + msg
								+ "\n");
					}
				}

			}
			// 捕捉到异常后，从Map中删除
			catch (IOException e) {
				clients.removeByValue(ps);
				// 关闭网络、IO资源
				try {
					if (br != null) {
						br.close();
					}
					if (out != null) {
						out.close();
					}
					if (socket != null) {
						socket.close();
					}
				} catch (IOException ee) {
				}
			}
		}

		// 将读到的内容去掉前后的特殊字符，得到真实信息
		private String getMessage(String line) {
			return line.substring(2, line.length() - 2);
		}
	}
}
