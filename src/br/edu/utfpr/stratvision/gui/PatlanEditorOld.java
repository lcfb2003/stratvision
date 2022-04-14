package br.edu.utfpr.stratvision.gui;

import br.ufpr.inf.heuchess.HeuChess;
import br.ufpr.inf.heuchess.representacao.estrategia.PatternEvaluator;
import br.edu.utfpr.stratvision.persistence.PatternDAO;
import br.ufpr.inf.heuchess.persistencia.TurmaDAO;
import br.ufpr.inf.heuchess.persistencia.UsuarioDAO;
import br.ufpr.inf.heuchess.representacao.estrategia.ScenarioShape;
import br.ufpr.inf.heuchess.representacao.estrategia.LRPRGParser;
import br.ufpr.inf.heuchess.representacao.estrategia.Pattern;
import br.ufpr.inf.heuchess.representacao.estrategia.ParseException;
import br.ufpr.inf.heuchess.representacao.estrategia.TokenMgrError;
import br.ufpr.inf.heuchess.representacao.heuristica.Etapa;
import br.ufpr.inf.heuchess.representacao.heuristica.Permissao;
import br.ufpr.inf.heuchess.representacao.organizacao.Usuario;
import br.edu.utfpr.stratvision.patlan.BitBoard;
import br.ufpr.inf.heuchess.representacao.situacaojogo.Tabuleiro;
import br.ufpr.inf.heuchess.telas.situacaojogo.PanelPosicaoJogoNew;
import br.edu.utfpr.stratvision.utils.TextLineNumber;
import br.edu.utfpr.stratvision.utilsgui.UtilsGUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * @author Luis Carlos F. Bueno - 08/04/2021
 */
public class PatlanEditorOld extends javax.swing.JFrame {

    JFrame mainForm;
    private boolean loading = false;
    private boolean isNew;
    public Pattern padrao;
    private Pattern originalPattern;
    private boolean isSaved, hasSintaticError;

    private Tabuleiro board = null;
    private BitBoard bitBoard;
    private String fen;
    private LRPRGParser parser;
    private PanelPosicaoJogoNew pos;
    private TextLineNumber tln;

    private static final Style styleNormal = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
    private static final SimpleAttributeSet styleNegritoPreto;    // Usado para destacar a��es gerais
    private static final SimpleAttributeSet styleNegritoAzul;     // Usado para destacar vantagem para Jogador 
    private static final SimpleAttributeSet styleNegritoLaranja;  // Usado para indicar igualdade (empate)
    private static final SimpleAttributeSet styleNegritoVermelho; // Usado para destacar vantagem para Oponente 

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    static {
        styleNegritoAzul = new SimpleAttributeSet();
        StyleConstants.setBold(styleNegritoAzul, true);
        StyleConstants.setForeground(styleNegritoAzul, Color.BLUE);

        styleNegritoVermelho = new SimpleAttributeSet();
        StyleConstants.setBold(styleNegritoVermelho, true);
        StyleConstants.setForeground(styleNegritoVermelho, Color.RED);

        styleNegritoLaranja = new SimpleAttributeSet();
        StyleConstants.setBold(styleNegritoLaranja, true);
        StyleConstants.setForeground(styleNegritoLaranja, Color.ORANGE);

        styleNegritoPreto = new SimpleAttributeSet();
        StyleConstants.setBold(styleNegritoPreto, true);
        StyleConstants.setForeground(styleNegritoPreto, Color.BLACK);
    }
    private boolean podeAlterar;
    private boolean podeAnotar;

    public PatlanEditorOld() {
        initComponents();
        this.setLocationRelativeTo(null);
        isNew = true;
        isSaved = true;
        tln = new TextLineNumber(txtEditor);
        scrollPadrao.setRowHeaderView(tln);
        ButtonGroup grpJogador = new ButtonGroup();
        optBrancas.getModel().setGroup(grpJogador);
        optPretas.getModel().setGroup(grpJogador);
        txtEditor.addCaretListener((CaretEvent e) -> {
            JTextArea editArea = (JTextArea) e.getSource();

            // Lets start with some default values for the line and column.
            int linenum = 1;
            int columnnum = 1;

            // We create a try catch to catch any exceptions. We will simply ignore such an error for our demonstration.
            try {
                // First we find the position of the caret. This is the number of where the caret is in relation to the start of the JTextArea
                // in the upper left corner. We use this position to find offset values (eg what line we are on for the given position as well as
                // what position that line starts on.
                int caretpos = editArea.getCaretPosition();
                linenum = editArea.getLineOfOffset(caretpos);

                // We subtract the offset of where our line starts from the overall caret position.
                // So lets say that we are on line 5 and that line starts at caret position 100, if our caret position is currently 106
                // we know that we must be on column 6 of line 5.
                columnnum = caretpos - editArea.getLineStartOffset(linenum);

                // We have to add one here because line numbers start at 0 for getLineOfOffset and we want it to start at 1 for display.
                linenum += 1;
            } catch (Exception ex) {
            }

            // Once we know the position of the line and the column, pass it to a helper function for updating the status bar.
            updateStatus(linenum, columnnum);
        });

        txtEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButtons();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButtons();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButtons();
            }
        });

        try {
            padrao = new Pattern("PadraoNovo", HeuChess.usuario.getId(), null);
            String template = "PADRAO \"Template\"\n"
                    + "AUTOR \"Nome\"\n"
                    + "DESCRICAO \"Descreva aqui o padrao\"\n"
                    + "PESO 1.0\n"
                    + "FORMATO\n"
                    + "\n"
                    + "CONDICAO\n"
                    + ";";
            txtEditor.setText(template);
            txtEditor.requestFocus();
            updateButtons();
        } catch (Exception ex) {

            HeuChess.registraExcecao(ex);

            UtilsGUI.dialogoErro(getParent(), "Erro ao criar novo padr�o estrat�gico\n" + ex.getMessage());
            dispose();
        }

        pos = new PanelPosicaoJogoNew(pnlPos, true);
        pnlPos.add(pos, BorderLayout.CENTER);
        pos.setLoading(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public PatlanEditorOld(Pattern padraoE) throws CloneNotSupportedException {
        this();
        originalPattern = padraoE.clone();
        try {
            padrao = padraoE.clone();
            if ((HeuChess.usuario.getId() == padrao.getIdAutor())
                    || (HeuChess.usuario.getTipo() == Usuario.ADMINISTRADOR)) {
                podeAlterar = true;
                podeAnotar = true;
            } else if (UsuarioDAO.coordenoTurma(HeuChess.usuario, padrao.getIdAutor()) != -1) {
                podeAlterar = true;
                podeAnotar = true;
            } else {
                ArrayList<Integer> permissoes = TurmaDAO.listaPermissoes(HeuChess.usuario, padrao.getIdAutor());

                for (Integer inteiro : permissoes) {
                    if (Permissao.ALTERAR.existe(inteiro.intValue())) {
                        podeAlterar = true;
                        break;
                    }
                }
                for (Integer inteiro : permissoes) {
                    if (Permissao.ANOTAR.existe(inteiro.intValue())) {
                        podeAnotar = true;
                        break;
                    }
                }
            }
        } catch (CloneNotSupportedException ex) {
            HeuChess.registraExcecao(ex);
            UtilsGUI.dialogoErro(getParent(), "Erro ao abrir padr�o estrat�gico\n" + ex.getMessage());
            dispose();
        } catch (Exception ex) {
            HeuChess.registraExcecao(ex);
            UtilsGUI.dialogoErro(getParent(), "Erro ao abrir padr�o estrat�gico\n" + ex.getMessage());
            dispose();
        }
        isNew = false;
        txtEditor.setText(padrao.getSource());
        setVisible(true);
    }
    
    public long idPadrao() {
        return padrao.getId();
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private methods">
    private void updateButtons() {
        btnBuscar.setEnabled(txtEditor.getText().length() > 0 && board != null);
        btnSave.setEnabled(txtEditor.getText().length() > 0);
    }

    private void updateStatus(int linenumber, int columnnumber) {
        lblLinCol.setText("Lin: " + linenumber + " Col: " + (columnnumber + 1));
    }
//</editor-fold>

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        btnAbrirFEN = new javax.swing.JButton();
        lblCodigo = new javax.swing.JLabel();
        lblLinCol = new javax.swing.JLabel();
        txtFEN = new javax.swing.JTextField();
        lblQuemJoga = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        pnlPos = new javax.swing.JPanel();
        scrollPadrao = new javax.swing.JScrollPane();
        txtEditor = new javax.swing.JTextArea();
        pnlResultados = new javax.swing.JTabbedPane();
        pnlSaida = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtMsg = new javax.swing.JTextPane();
        pnlInstancias = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstInstancias = new javax.swing.JList<>();
        jScrollPane4 = new javax.swing.JScrollPane();
        lstInstancia = new javax.swing.JList<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        lstInstanciasV = new javax.swing.JList<>();
        jScrollPane6 = new javax.swing.JScrollPane();
        lstInstanciaV = new javax.swing.JList<>();
        jLabel3 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        btnSave = new javax.swing.JButton();
        btnBuscar = new javax.swing.JButton();
        btnSintaxe = new javax.swing.JButton();
        btnAddFormato = new javax.swing.JButton();
        btnAddCond = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        optBrancas = new javax.swing.JRadioButton();
        optPretas = new javax.swing.JRadioButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Pattern authoring tool");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel1.setPreferredSize(new java.awt.Dimension(1024, 58));

        btnAbrirFEN.setText("Load FEN position");
        btnAbrirFEN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAbrirFENActionPerformed(evt);
            }
        });

        lblCodigo.setText("Source code - HLP definition");

        lblLinCol.setText("Row: 000 Col: 000");

        txtFEN.setText("FEN");

        lblQuemJoga.setText("BRANCAS");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(txtFEN)
                        .addGap(18, 18, 18)
                        .addComponent(btnAbrirFEN, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lblQuemJoga, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 209, Short.MAX_VALUE)
                        .addComponent(lblCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(119, 119, 119)
                        .addComponent(lblLinCol, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(166, 166, 166))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFEN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAbrirFEN, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLinCol, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblQuemJoga, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        pnlPos.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pnlPos.setPreferredSize(new java.awt.Dimension(300, 280));
        pnlPos.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanel2.add(pnlPos, gridBagConstraints);

        txtEditor.setColumns(40);
        txtEditor.setRows(5);
        txtEditor.setTabSize(3);
        txtEditor.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtEditorFocusGained(evt);
            }
        });
        txtEditor.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtEditorKeyReleased(evt);
            }
        });
        scrollPadrao.setViewportView(txtEditor);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 2.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanel2.add(scrollPadrao, gridBagConstraints);

        jScrollPane2.setViewportView(txtMsg);

        javax.swing.GroupLayout pnlSaidaLayout = new javax.swing.GroupLayout(pnlSaida);
        pnlSaida.setLayout(pnlSaidaLayout);
        pnlSaidaLayout.setHorizontalGroup(
            pnlSaidaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1019, Short.MAX_VALUE)
        );
        pnlSaidaLayout.setVerticalGroup(
            pnlSaidaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE)
        );

        pnlResultados.addTab("Output", pnlSaida);

        lstInstancias.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lstInstancias.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstInstancias.setToolTipText("Clique sobre a instância para destacá-la no tabuleiro");
        lstInstancias.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstInstanciasValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(lstInstancias);

        lstInstancia.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lstInstancia.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstInstancia.setToolTipText("Clique sobre a linha para destacar no tabuleiro");
        lstInstancia.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstInstanciaValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(lstInstancia);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Found scenario instances");

        lstInstanciasV.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lstInstanciasV.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstInstanciasV.setToolTipText("Clique sobre a instância para destacá-la no tabuleiro");
        lstInstanciasV.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstInstanciasVValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(lstInstanciasV);

        lstInstanciaV.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lstInstanciaV.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstInstanciaV.setToolTipText("Clique sobre a linha para destacar no tabuleiro");
        lstInstanciaV.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstInstanciaVValueChanged(evt);
            }
        });
        jScrollPane6.setViewportView(lstInstanciaV);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Validated instances");

        javax.swing.GroupLayout pnlInstanciasLayout = new javax.swing.GroupLayout(pnlInstancias);
        pnlInstancias.setLayout(pnlInstanciasLayout);
        pnlInstanciasLayout.setHorizontalGroup(
            pnlInstanciasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlInstanciasLayout.createSequentialGroup()
                .addGroup(pnlInstanciasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlInstanciasLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnlInstanciasLayout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addGroup(pnlInstanciasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlInstanciasLayout.createSequentialGroup()
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        pnlInstanciasLayout.setVerticalGroup(
            pnlInstanciasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlInstanciasLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(pnlInstanciasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlInstanciasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6))
                .addGap(31, 31, 31))
        );

        pnlResultados.addTab("Found instances", pnlInstancias);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.7;
        jPanel2.add(pnlResultados, gridBagConstraints);

        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        btnSave.setText("Save pattern");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnBuscar.setText("Search");
        btnBuscar.setToolTipText("Tenta encontrar o padrão na posição escolhida");
        btnBuscar.setEnabled(false);
        btnBuscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarActionPerformed(evt);
            }
        });

        btnSintaxe.setText("Syntax check");
        btnSintaxe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSintaxeActionPerformed(evt);
            }
        });

        btnAddFormato.setText("Add relations...");
        btnAddFormato.setToolTipText("Adiciona uma linha ao formato do padrão");
        btnAddFormato.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddFormatoActionPerformed(evt);
            }
        });

        btnAddCond.setText("Add conditions..");
        btnAddCond.setToolTipText("Adiciona uma linha ao formato do padrão");
        btnAddCond.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddCondActionPerformed(evt);
            }
        });

        jLabel1.setText("Test mode:");

        optBrancas.setText("WHITE");

        optPretas.setText("BLACK");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnSave, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddFormato, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddCond, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSintaxe, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuscar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(optPretas)
                            .addComponent(optBrancas))))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnSave)
                .addGap(18, 18, 18)
                .addComponent(btnAddFormato)
                .addGap(18, 18, 18)
                .addComponent(btnAddCond)
                .addGap(18, 18, 18)
                .addComponent(btnSintaxe)
                .addGap(30, 30, 30)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(optBrancas)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(optPretas)
                .addGap(33, 33, 33)
                .addComponent(btnBuscar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.add(jPanel3, new java.awt.GridBagConstraints());

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtEditorFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtEditorFocusGained
        btnSave.setEnabled(txtEditor.getText().length() > 0);
    }//GEN-LAST:event_txtEditorFocusGained

    private void txtEditorKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtEditorKeyReleased
        btnSave.setEnabled(txtEditor.getText().length() > 0);
        isSaved = false;
        if (evt.isControlDown() && evt.getKeyChar() == '+') {
            Font font = txtEditor.getFont();
            float size = font.getSize() + 1.0f;
            txtEditor.setFont(font.deriveFont(size));
        } else if (evt.isControlDown() && evt.getKeyChar() == '-') {
            Font font = txtEditor.getFont();
            float size = font.getSize() - 1.0f;
            txtEditor.setFont(font.deriveFont(size));
        }
    }//GEN-LAST:event_txtEditorKeyReleased

    private void lstInstanciasValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstInstanciasValueChanged
        if (loading) {
            return;
        }

        DefaultListModel<String> model = new DefaultListModel<>();
        padrao.Instancias.get(lstInstancias.getSelectedIndex()).stream().forEach((f) -> {
            model.addElement(f.toString());
        });
        lstInstancia.setModel(model);
        pos.limpaLinhas();
        pos.mostraInstancia(padrao, lstInstancias.getSelectedIndex(), -1, false);
        // pnlPos.invalidate();
    }//GEN-LAST:event_lstInstanciasValueChanged

    private void lstInstanciaValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstInstanciaValueChanged
        if (loading) {
            return;
        }
        pos.limpaLinhas();
        pos.mostraInstancia(padrao, lstInstancias.getSelectedIndex(), lstInstancia.getSelectedIndex(), false);
    }//GEN-LAST:event_lstInstanciaValueChanged

    private void lstInstanciasVValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstInstanciasVValueChanged
        if (loading) {
            return;
        }
        DefaultListModel<String> model = new DefaultListModel<>();
        boolean or = false;
        for (ScenarioShape f : padrao.InstanciasOK.get(lstInstanciasV.getSelectedIndex())) {
            if (f.orIni && !or) {
                model.addElement("{");
                or = true;
            }
            if (f.orFim) {
                or = false;
            }
            model.addElement((or || f.orFim ? "   " : "") + f.toString() + (or ? " OU " : ""));
            if (f.orFim) {
                model.addElement("}");
            }
        }
        lstInstanciaV.setModel(model);
        pos.limpaLinhas();
        pos.mostraInstancia(padrao, lstInstanciasV.getSelectedIndex(), -1, true);
    }//GEN-LAST:event_lstInstanciasVValueChanged

    private void lstInstanciaVValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstInstanciaVValueChanged
        if (loading) {
            return;
        }
        pos.limpaLinhas();
        pos.mostraInstancia(padrao, lstInstanciasV.getSelectedIndex(), lstInstanciaV.getSelectedIndex(), true);
    }//GEN-LAST:event_lstInstanciaVValueChanged

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        try {
            padrao.setSource(txtEditor.getText());
            if (isNew) {
                PatternDAO.adiciona(padrao);
                isNew = false;
            } else {
                PatternDAO.atualiza(padrao);
            }
            isSaved = true;
        } catch (IOException ex) {
            Logger.getLogger(PatlanEditorOld.class.getName()).log(Level.SEVERE, null, ex);
            UtilsGUI.dialogoErro(this, "Erro ao gravar arquivo na base de fatos: \n" + ex.getMessage());
        } catch (Exception ex) {
            Logger.getLogger(PatlanEditorOld.class.getName()).log(Level.SEVERE, null, ex);
            UtilsGUI.dialogoErro(this, "Erro ao buscar padr�o: \n" + ex.getMessage());
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        try {
            txtMsg.setText("");
            pos.limpaLinhas();
            pnlPos.invalidate();
            padrao.setSource(txtEditor.getText());
            btnSintaxe.doClick();
            if (hasSintaticError) {
                return;
            }
            PatternEvaluator avalia = new PatternEvaluator(optBrancas.getModel().isSelected(),
                    null, txtMsg, PatternEvaluator.AUTHORING_MODE);
            Etapa etapaI = new Etapa(null, "Temp", HeuChess.usuario.getId(), null, "1,5,3,3,9");
            avalia.setGamePhase(etapaI);
            //se existe um conjunto minimo e a posi��o cont�m o conjunto ou se
            //n�o existe conjunto m�nimo e o material presente na posi��o atende ao especificado no padr�o
            if (!padrao.getExclusiveSet().isEmpty() && avalia.SETOFPIECES(padrao.getExclusiveSet(), bitBoard)
                    || (padrao.getExclusiveSet().isEmpty() && avalia.MATERIAL(padrao.getST(), bitBoard))) {
                avalia.geraBaseFatos(bitBoard); //chama o gerador de fatos para a posi��o em an�lise
                avalia.avaliaPadrao(padrao);
                bitBoard = avalia.getBitboard(); // restaura o bitBoard caso haja movimentos previstos
                loading = true;
                if (padrao.Instancias.size() > 0) { //preenche a lista instancias brutas do padr�o (formato combina)
                    DefaultListModel<String> model = new DefaultListModel<>();
                    for (int ic = 1; ic <= padrao.Instancias.size(); ic++) {
                        model.addElement(String.valueOf(ic));
                    }
                    lstInstancias.setModel(model);
                    pnlResultados.setSelectedIndex(1);
                } else {
                    lstInstancias.setModel(new DefaultListModel<>());
                    lstInstancia.setModel(new DefaultListModel<>());
                    pnlResultados.setSelectedIndex(0);
                }
                if (padrao.InstanciasOK.size() > 0) { //preenche a lista com as instancias que passaram pelas condi��es
                    DefaultListModel<String> model = new DefaultListModel<>();
                    for (int ic = 1; ic <= padrao.InstanciasOK.size(); ic++) {
                        model.addElement(String.valueOf(ic));
                    }
                    lstInstanciasV.setModel(model);
                    pnlResultados.setSelectedIndex(1);
                } else {
                    lstInstanciasV.setModel(new DefaultListModel<>());
                    lstInstanciaV.setModel(new DefaultListModel<>());
                    pnlResultados.setSelectedIndex(0);
                }
            } else {
                UtilsGUI.addFormatedText(txtMsg, "\nTabuleiro n�o cont�m o conjunto de pe�as requerido no padr�o!", styleNormal);
            }
            avalia.dispose();
            loading = false;
        } catch (IOException ex) {
            Logger.getLogger(PatlanEditorOld.class.getName()).log(Level.SEVERE, null, ex);
            UtilsGUI.dialogoErro(this, "Erro ao gravar arquivo na base de fatos: \n" + ex.getMessage());
            pnlResultados.setSelectedIndex(0);
        } catch (Exception ex) {
            Logger.getLogger(PatlanEditorOld.class.getName()).log(Level.SEVERE, null, ex);
            UtilsGUI.dialogoErro(this, "Erro ao buscar padr�o: \n" + ex.getMessage());
            pnlResultados.setSelectedIndex(0);
        }
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnSintaxeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSintaxeActionPerformed
        btnSintaxe.setEnabled(false);
        hasSintaticError = false;
        this.txtMsg.setText("");
        InputStream stream = new ByteArrayInputStream(this.txtEditor.getText().getBytes(StandardCharsets.UTF_8));
        //if(primeiraExecucao)
        parser = null;
        parser = new LRPRGParser(stream);// else parser.ReInit(stream);
        try {
            padrao.reset();
            parser.pattern = padrao;
            parser.Patterns();
//            padrao.setControleFormato(parser.controleFormato);
//            padrao.setCondicaoToJava(parser.condJava);
            padrao.setST(parser.ST);
            padrao.prepareStatement();
            UtilsGUI.addFormatedText(txtMsg, "\nTudo certo!", styleNormal);
        } catch (ParseException ex) {
            hasSintaticError = true;
            UtilsGUI.addFormatedText(txtMsg, "\n" + ex.getMessage(), styleNormal);
            Logger.getLogger(PatlanEditorOld.class.getName()).log(Level.SEVERE, null, ex);
            pnlResultados.setSelectedIndex(0);
            try {
                int startOffSet = txtEditor.getLineStartOffset(parser.token.beginLine - 1);
                txtEditor.select(startOffSet, startOffSet);
                txtEditor.requestFocus();
            } catch (BadLocationException ex1) {
                Logger.getLogger(PatlanEditorOld.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (TokenMgrError ex) {
            UtilsGUI.addFormatedText(txtMsg, "\n" + ex.getMessage(), styleNormal);
            Logger.getLogger(PatlanEditorOld.class.getName()).log(Level.SEVERE, null, ex);
            pnlResultados.setSelectedIndex(0);
        }
        pnlResultados.setSelectedIndex(0);
        btnSintaxe.setEnabled(true);
    }//GEN-LAST:event_btnSintaxeActionPerformed

    private void btnAddFormatoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddFormatoActionPerformed
        btnSintaxe.doClick();
        TelaNovoComponente form = new TelaNovoComponente(TelaNovoComponente.TipoComponente.FORMATO, txtEditor, parser);
        form.addListeners();
        form.setVisible(true);
    }//GEN-LAST:event_btnAddFormatoActionPerformed

    private void btnAddCondActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddCondActionPerformed
        btnSintaxe.doClick();
        TelaNovoComponente form = new TelaNovoComponente(TelaNovoComponente.TipoComponente.FUNCAO, txtEditor, parser);
        form.addListeners();
        form.setVisible(true);
    }//GEN-LAST:event_btnAddCondActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (!isSaved) {
            int resp = UtilsGUI.dialogoConfirmacao(this, "Padr�o n�o foi salvo ainda. Deseja fechar assim mesmo?", "Aten��o");
            if (resp == JOptionPane.YES_OPTION) {
                this.dispose();
            }
        } else {
            this.dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    // <editor-fold defaultstate="collapsed" desc="Eventos de componentes">   
    private void btnAbrirFENActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAbrirFENActionPerformed
        try {
            board = new Tabuleiro(txtFEN.getText());
            optBrancas.getModel().setSelected(board.isWhiteActive());
            optPretas.getModel().setSelected(!board.isWhiteActive());
            lblQuemJoga.setText(board.isWhiteActive() ? "BRANCAS JOGAM" : "PRETAS JOGAM");
            fen = txtFEN.getText();
            bitBoard = new BitBoard();
            bitBoard.importFEN(txtFEN.getText());
            pos.limpaLinhas();
            pos.bits = bitBoard.bits.Clone();
            pos.drawPieces();
            // pos.resetPanels();
            pos.repaint();
            btnBuscar.setEnabled(true);
        } catch (Exception ex) {
            Logger.getLogger(PatlanEditorOld.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnAbrirFENActionPerformed
// </editor-fold> 


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAbrirFEN;
    private javax.swing.JButton btnAddCond;
    private javax.swing.JButton btnAddFormato;
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnSintaxe;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JLabel lblCodigo;
    private javax.swing.JLabel lblLinCol;
    private javax.swing.JLabel lblQuemJoga;
    private javax.swing.JList<String> lstInstancia;
    private javax.swing.JList<String> lstInstanciaV;
    private javax.swing.JList<String> lstInstancias;
    private javax.swing.JList<String> lstInstanciasV;
    private javax.swing.JRadioButton optBrancas;
    private javax.swing.JRadioButton optPretas;
    private javax.swing.JPanel pnlInstancias;
    private javax.swing.JPanel pnlPos;
    private javax.swing.JTabbedPane pnlResultados;
    private javax.swing.JPanel pnlSaida;
    private javax.swing.JScrollPane scrollPadrao;
    private javax.swing.JTextArea txtEditor;
    private javax.swing.JTextField txtFEN;
    private javax.swing.JTextPane txtMsg;
    // End of variables declaration//GEN-END:variables
}
