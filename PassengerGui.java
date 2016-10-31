/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
*****************************************************************/

package carpooling;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
  @author Giovanni Caire - TILAB
 */
class PassengerGui extends JFrame {	
    private PassengerAgent myAgent;

    private JTextField originField, destinyField, departureTime, arrivalTime;

    PassengerGui(PassengerAgent a) {
        super(a.getLocalName());

        myAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(4, 4));
        p.add(new JLabel("Lugar de salida:"));
        originField = new JTextField(15);
        p.add(originField);
        p.add(new JLabel("Lugar de llegada:"));
        destinyField = new JTextField(15);
        p.add(destinyField);
        p.add(new JLabel("Hora de llegada:"));
        arrivalTime = new JTextField(15);
        p.add(arrivalTime);
        getContentPane().add(p, BorderLayout.CENTER);

        JButton addButton = new JButton("Ask for ride");
        addButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String of = originField.getText().trim();
                    String df = destinyField.getText().trim();
                    String at = arrivalTime.getText().trim();
                    myAgent.askForRide(of, df, at);                    
                    setVisible(false);
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(PassengerGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
                }
            }
        } );
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes 
        // the GUI using the button on the upper right corner	
        addWindowListener(new	WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                        myAgent.doDelete();
                }
        } );

        setResizable(false);
    }
	
	public void showGui() {
            pack();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int centerX = (int)screenSize.getWidth() / 2;
            int centerY = (int)screenSize.getHeight() / 2;
            setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
            super.setVisible(true);
	}
}
