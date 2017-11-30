import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.IS2.Nikola.ExcepcionEnTrabajo;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Color;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.Insets;

public class ProcesaFrame extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ProcesaFrame frame = new ProcesaFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ProcesaFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 150, 100);
		contentPane = new JPanel();
		contentPane.setBackground(UIManager.getColor("Panel.background"));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JButton btnProcesado = new JButton("Procesado");
		btnProcesado.setMargin(new Insets(1, 1, 1, 1));
		btnProcesado.setMinimumSize(new Dimension(30, 15));
		btnProcesado.setMaximumSize(new Dimension(30, 15));
		btnProcesado.setBackground(UIManager.getColor("TextField.selectionBackground"));
		btnProcesado.setForeground(UIManager.getColor("PopupMenu.foreground"));
		btnProcesado.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					ProcesaTexto procesa = new ProcesaTexto("config.txt");
				} catch (ExcepcionEnTrabajo e) {
					System.out.println("Hay un problema con el fichero de configuración");
				}
			}
		});
		contentPane.add(btnProcesado, BorderLayout.CENTER);
	}

}
