package gastos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/** Mis Gastos: control personal de gastos e ingresos. */
public class Main extends JFrame {
    private final Almacen almacen = new Almacen();
    private int semana = 0;
    private List<Gasto> visibles = new ArrayList<>();

    private final JTextField monto = new JTextField(7);
    private final JTextField nota = new JTextField(14);
    private final JTextField buscarTexto = new JTextField(14);
    private final JComboBox<String> tipo = new JComboBox<>(new String[]{"Gasto", "Ingreso"});
    private final JComboBox<String> cat = new JComboBox<>();
    private final JComboBox<String> filtroCategoria = new JComboBox<>();
    private final JLabel rango = new JLabel("", SwingConstants.CENTER);
    private final JLabel total = new JLabel();
    private final JLabel comparacion = new JLabel();
    private final JLabel balance = new JLabel();
    private final JLabel presupuesto = new JLabel();
    private final JPanel resumen = new JPanel(new GridLayout(0, 1, 0, 6));
    private final JPanel grafico = new GraficoPanel();
    private final JButton anterior = new JButton("←");
    private final JButton siguiente = new JButton("→");
    private final JButton buscar = new JButton("Buscar");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Fecha", "Tipo", "Categoría", "Nota", "Monto"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);

    public Main() {
        super("Mis Gastos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        cargarCategorias();

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton guardar = new JButton("Guardar");
        JButton nuevaCategoria = new JButton("Nueva categoría");
        form.add(new JLabel("Tipo:"));
        form.add(tipo);
        form.add(new JLabel("Monto:"));
        form.add(monto);
        form.add(cat);
        form.add(new JLabel("Nota:"));
        form.add(nota);
        form.add(guardar);
        form.add(nuevaCategoria);
        add(form, BorderLayout.NORTH);

        JPanel nav = new JPanel(new BorderLayout());
        nav.add(anterior, BorderLayout.WEST);
        nav.add(rango, BorderLayout.CENTER);
        nav.add(siguiente, BorderLayout.EAST);
        total.setFont(total.getFont().deriveFont(Font.BOLD, 28f));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Buscar:"));
        filtros.add(buscarTexto);
        filtros.add(filtroCategoria);
        filtros.add(buscar);

        JPanel semanal = new JPanel();
        semanal.setLayout(new BoxLayout(semanal, BoxLayout.Y_AXIS));
        semanal.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        semanal.add(nav);
        semanal.add(total);
        semanal.add(comparacion);
        semanal.add(balance);
        semanal.add(presupuesto);
        semanal.add(javax.swing.Box.createVerticalStrut(8));
        semanal.add(filtros);
        semanal.add(resumen);
        semanal.add(grafico);

        JButton editar = new JButton("Editar gasto seleccionado");
        JButton eliminar = new JButton("Eliminar seleccionado");
        JButton configurarPresupuesto = new JButton("Configurar presupuesto");
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(configurarPresupuesto);
        acciones.add(editar);
        acciones.add(eliminar);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        centro.add(semanal, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        centro.add(acciones, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        guardar.addActionListener(e -> guardar());
        monto.addActionListener(e -> guardar());
        editar.addActionListener(e -> editar());
        eliminar.addActionListener(e -> eliminar());
        nuevaCategoria.addActionListener(e -> nuevaCategoria());
        configurarPresupuesto.addActionListener(e -> configurarPresupuesto());
        buscar.addActionListener(e -> refrescar());
        buscarTexto.addActionListener(e -> refrescar());
        filtroCategoria.addActionListener(e -> refrescar());
        anterior.addActionListener(e -> {
            semana--;
            refrescar();
        });
        siguiente.addActionListener(e -> {
            semana++;
            refrescar();
        });
        tipo.addActionListener(e -> actualizarCategoriaSegunTipo());

        refrescar();
        setSize(760, 760);
        setLocationRelativeTo(null);
    }

    private void cargarCategorias() {
        cat.removeAllItems();
        filtroCategoria.removeAllItems();
        filtroCategoria.addItem("Todas");
        for (String categoria : almacen.getCategorias()) {
            cat.addItem(categoria);
            filtroCategoria.addItem(categoria);
        }
    }

    private void actualizarCategoriaSegunTipo() {
        if ("Ingreso".equals(tipo.getSelectedItem()) && cat.getItemCount() > 0) {
            cat.setSelectedItem("Ingresos");
        }
    }

    private void nuevaCategoria() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la categoría:");
        if (nombre == null || nombre.trim().isEmpty()) return;
        almacen.agregarCategoria(nombre);
        cargarCategorias();
        cat.setSelectedItem(nombre.trim());
    }

    private void configurarPresupuesto() {
        String actual = Double.toString(almacen.getPresupuestoSemanal());
        String valor = JOptionPane.showInputDialog(this, "Presupuesto semanal:", actual);
        if (valor == null) return;
        try {
            double presupuestoValor = Double.parseDouble(valor.trim().replace(',', '.'));
            almacen.setPresupuestoSemanal(presupuestoValor);
            refrescar();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Introduce un monto válido.");
        }
    }

    private void guardar() {
        double m;
        try {
            m = Double.parseDouble(monto.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            m = 0;
        }
        if (m <= 0) {
            JOptionPane.showMessageDialog(this, "Escribe un monto mayor que cero.",
                    "Monto inválido", JOptionPane.WARNING_MESSAGE);
            monto.requestFocus();
            return;
        }
        String categoria = (String) cat.getSelectedItem();
        String tipoMovimiento = (String) tipo.getSelectedItem();
        almacen.agregar(Math.round(m * 100) / 100.0, categoria, nota.getText().trim(),
                LocalDate.now(), tipoMovimiento);
        monto.setText("");
        nota.setText("");
        semana = 0;
        refrescar();
        monto.requestFocus();
    }

    private void editar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila de la lista.");
            return;
        }
        Gasto g = visibles.get(fila);

        JTextField montoEdit = new JTextField(Double.toString(g.monto));
        JTextField notaEdit = new JTextField(g.nota);
        JTextField fechaEdit = new JTextField(g.fecha.toString());
        JComboBox<String> tipoEdit = new JComboBox<>(new String[]{"Gasto", "Ingreso"});
        tipoEdit.setSelectedItem(g.tipo);
        JComboBox<String> categoriaEdit = new JComboBox<>();
        for (String categoria : almacen.getCategorias()) categoriaEdit.addItem(categoria);
        categoriaEdit.setSelectedItem(g.cat);

        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 6));
        panel.add(new JLabel("Tipo:"));
        panel.add(tipoEdit);
        panel.add(new JLabel("Monto:"));
        panel.add(montoEdit);
        panel.add(new JLabel("Categoría:"));
        panel.add(categoriaEdit);
        panel.add(new JLabel("Nota:"));
        panel.add(notaEdit);
        panel.add(new JLabel("Fecha (AAAA-MM-DD):"));
        panel.add(fechaEdit);

        if (JOptionPane.showConfirmDialog(this, panel, "Editar movimiento",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            double nuevoMonto = Double.parseDouble(montoEdit.getText().trim().replace(',', '.'));
            LocalDate nuevaFecha = LocalDate.parse(fechaEdit.getText().trim());
            if (nuevoMonto <= 0) throw new NumberFormatException();
            almacen.actualizar(g.id, Math.round(nuevoMonto * 100) / 100.0,
                    (String) categoriaEdit.getSelectedItem(), notaEdit.getText().trim(),
                    nuevaFecha, (String) tipoEdit.getSelectedItem());
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Revisa el monto, la fecha y el formato AAAA-MM-DD.", "Datos inválidos",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un movimiento de la lista.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar este movimiento?", "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            almacen.eliminar(visibles.get(fila).id);
            refrescar();
        }
    }

    private static double sumaTipo(List<Gasto> lista, String tipoMovimiento) {
        double suma = 0;
        for (Gasto g : lista) {
            if (tipoMovimiento.equals(g.tipo)) suma += g.monto;
        }
        return suma;
    }

    private static String dinero(double d) {
        return String.format(Locale.US, "$%.2f", d);
    }

    private void refrescar() {
        LocalDate lunes = Almacen.lunes(semana);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("d MMM", new Locale("es"));
        rango.setText(lunes.format(f) + " – " + lunes.plusDays(6).format(f));
        siguiente.setEnabled(semana < 0);

        String categoria = (String) filtroCategoria.getSelectedItem();
        visibles = almacen.buscar(lunes, buscarTexto.getText(), categoria);
        double gastos = sumaTipo(visibles, "Gasto");
        double ingresos = sumaTipo(visibles, "Ingreso");
        total.setText(dinero(gastos));

        LocalDate lunesAnterior = Almacen.lunes(semana - 1);
        double gastoAnterior = sumaTipo(almacen.delSemana(lunesAnterior), "Gasto");
        if (gastoAnterior == 0) {
            comparacion.setText("Sin datos de gastos de la semana anterior");
        } else {
            long porcentaje = Math.round(Math.abs(gastos - gastoAnterior) / gastoAnterior * 100);
            comparacion.setText((gastos >= gastoAnterior ? "▲ " : "▼ ") +
                    porcentaje + "% frente a la semana anterior (" + dinero(gastoAnterior) + ")");
        }
        balance.setText("Ingresos: " + dinero(ingresos) + "  |  Saldo: " + dinero(ingresos - gastos));

        double limite = almacen.getPresupuestoSemanal();
        if (limite > 0) {
            double disponible = limite - gastos;
            presupuesto.setText(disponible >= 0
                    ? "Presupuesto semanal: " + dinero(disponible) + " disponibles"
                    : "Presupuesto semanal excedido por " + dinero(-disponible));
            presupuesto.setForeground(disponible >= 0
                    ? new Color(20, 120, 60)
                    : new Color(190, 40, 40));
        } else {
            presupuesto.setText("Sin presupuesto semanal configurado");
            presupuesto.setForeground(Color.DARK_GRAY);
        }

        Map<String, Double> porCategoria = new LinkedHashMap<>();
        for (Gasto g : visibles) {
            if ("Gasto".equals(g.tipo)) porCategoria.merge(g.cat, g.monto, Double::sum);
        }
        List<Map.Entry<String, Double>> filas = new ArrayList<>(porCategoria.entrySet());
        filas.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        resumen.removeAll();
        if (filas.isEmpty()) resumen.add(new JLabel("Aún no hay gastos esta semana"));
        for (Map.Entry<String, Double> e : filas) {
            int porcentaje = gastos == 0 ? 0 : (int) Math.round(e.getValue() / gastos * 100);
            JProgressBar barra = new JProgressBar(0, 100);
            barra.setValue(porcentaje);
            JPanel fila = new JPanel(new BorderLayout(8, 0));
            fila.add(new JLabel(e.getKey() + ": " + dinero(e.getValue()) + " (" + porcentaje + "%)"),
                    BorderLayout.NORTH);
            fila.add(barra, BorderLayout.CENTER);
            resumen.add(fila);
        }
        resumen.revalidate();
        resumen.repaint();

        modelo.setRowCount(0);
        for (Gasto g : visibles) {
            modelo.addRow(new Object[]{g.fecha, g.tipo, g.cat, g.nota, dinero(g.monto)});
        }
        ((GraficoPanel) grafico).setDatos(porCategoria);
        grafico.revalidate();
        grafico.repaint();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Se conserva el aspecto predeterminado.
        }
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }

    private static class GraficoPanel extends JPanel {
        private Map<String, Double> datos = new LinkedHashMap<>();

        GraficoPanel() {
            setPreferredSize(new Dimension(600, 170));
            setBorder(BorderFactory.createTitledBorder("Gastos por categoría"));
        }

        void setDatos(Map<String, Double> datos) {
            this.datos = new LinkedHashMap<>(datos);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (datos.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            double maximo = 1;
            for (double valor : datos.values()) maximo = Math.max(maximo, valor);

            int x = 20;
            int ancho = Math.max(18, (getWidth() - 40) / datos.size() - 10);
            int altoDisponible = getHeight() - 38;
            Color[] colores = {new Color(70, 130, 180), new Color(220, 120, 70),
                    new Color(90, 160, 100), new Color(180, 100, 160),
                    new Color(210, 170, 70), new Color(120, 120, 120)};
            int i = 0;
            for (Map.Entry<String, Double> e : datos.entrySet()) {
                int alto = (int) Math.round(e.getValue() / maximo * altoDisponible);
                int y = getHeight() - 28 - alto;
                g2.setColor(colores[i % colores.length]);
                g2.fillRect(x, y, ancho, alto);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(e.getKey(), x, getHeight() - 10);
                g2.drawString(dinero(e.getValue()), x, y - 4);
                x += ancho + 10;
                i++;
            }
        }
    }
}
