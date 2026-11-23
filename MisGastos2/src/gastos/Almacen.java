package gastos;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/** Guarda y lee los gastos en ~/.mis-gastos/gastos.csv (formato: id;monto;categoria;fecha;nota). */
public class Almacen {
    private final List<Gasto> gastos = new ArrayList<>();
    private final Path archivo = Paths.get(System.getProperty("user.home"), ".mis-gastos", "gastos.csv");
    private final Path archivoCategorias = Paths.get(System.getProperty("user.home"), ".mis-gastos", "categorias.csv");
    private final Path archivoConfig = Paths.get(System.getProperty("user.home"), ".mis-gastos", "config.properties");
    private final List<String> categorias = new ArrayList<>();
    private double presupuestoSemanal;

    public Almacen() {
        cargar();
        cargarCategorias();
        cargarConfig();
    }

    public static LocalDate lunes(int semanasAtras) {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(semanasAtras);
    }

    public void agregar(double monto, String cat, String nota) {
        agregar(monto, cat, nota, LocalDate.now(), "Gasto");
    }

    public void agregar(double monto, String cat, String nota, LocalDate fecha, String tipo) {
        gastos.add(new Gasto(System.currentTimeMillis(), monto, cat, nota, fecha, tipo));
        guardar();
    }

    public void actualizar(long id, double monto, String cat, String nota, LocalDate fecha, String tipo) {
        for (int i = 0; i < gastos.size(); i++) {
            Gasto g = gastos.get(i);
            if (g.id == id) {
                gastos.set(i, new Gasto(id, monto, cat, nota, fecha, tipo));
                break;
            }
        }
        guardar();
    }

    public void eliminar(long id) {
        gastos.removeIf(g -> g.id == id);
        guardar();
    }

    /** Gastos de la semana que empieza en el lunes indicado, del más reciente al más antiguo. */
    public List<Gasto> delSemana(LocalDate lunes) {
        LocalDate domingo = lunes.plusDays(6);
        List<Gasto> r = new ArrayList<>();
        for (Gasto g : gastos) {
            if (!g.fecha.isBefore(lunes) && !g.fecha.isAfter(domingo)) r.add(g);
        }
        r.sort((x, y) -> Long.compare(y.id, x.id));
        return r;
    }

    public List<Gasto> buscar(LocalDate lunes, String texto, String categoria) {
        LocalDate domingo = lunes.plusDays(6);
        List<Gasto> r = new ArrayList<>();
        String busqueda = texto == null ? "" : texto.trim().toLowerCase();
        for (Gasto g : gastos) {
            if (g.fecha.isBefore(lunes) || g.fecha.isAfter(domingo)) continue;
            if (categoria != null && !"Todas".equals(categoria) && !g.cat.equals(categoria)) continue;
            if (!busqueda.isEmpty() &&
                    !g.nota.toLowerCase().contains(busqueda) &&
                    !g.cat.toLowerCase().contains(busqueda)) continue;
            r.add(g);
        }
        r.sort((x, y) -> Long.compare(y.id, x.id));
        return r;
    }

    public List<String> getCategorias() {
        return new ArrayList<>(categorias);
    }

    public void agregarCategoria(String categoria) {
        String limpia = categoria == null ? "" : categoria.trim();
        if (!limpia.isEmpty() && categorias.stream().noneMatch(c -> c.equalsIgnoreCase(limpia))) {
            categorias.add(limpia);
            guardarCategorias();
        }
    }

    public double getPresupuestoSemanal() {
        return presupuestoSemanal;
    }

    public void setPresupuestoSemanal(double presupuesto) {
        presupuestoSemanal = Math.max(0, presupuesto);
        guardarConfig();
    }

    public double sumaTipo(List<Gasto> lista, String tipo) {
        double suma = 0;
        for (Gasto g : lista) {
            if (tipo.equals(g.tipo)) suma += g.monto;
        }
        return suma;
    }

    private void cargar() {
        if (!Files.exists(archivo)) return;
        try (BufferedReader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String l;
            while ((l = r.readLine()) != null) {
                String[] p = l.split(";", 6);
                if (p.length < 5) continue;
                try {
                    String tipo = p.length >= 6 && "Ingreso".equalsIgnoreCase(p[5]) ? "Ingreso" : "Gasto";
                    gastos.add(new Gasto(Long.parseLong(p[0]), Double.parseDouble(p[1]), p[2], p[4], LocalDate.parse(p[3]), tipo));
                } catch (RuntimeException e) {
                    // línea dañada: se ignora
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
        }
    }

    private void guardar() {
        try {
            Files.createDirectories(archivo.getParent());
            List<String> lineas = new ArrayList<>();
            for (Gasto g : gastos) {
                lineas.add(g.id + ";" + g.monto + ";" + g.cat + ";" + g.fecha + ";" +
                        g.nota.replace('\n', ' ').replace(';', ',') + ";" + g.tipo);
            }
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar " + archivo + ": " + e.getMessage());
        }
    }

    private void cargarCategorias() {
        if (Files.exists(archivoCategorias)) {
            try {
                categorias.addAll(Files.readAllLines(archivoCategorias, StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.err.println("No se pudieron leer las categorías: " + e.getMessage());
            }
        }
        if (categorias.isEmpty()) {
            categorias.addAll(Arrays.asList("Comida", "Transporte", "Estudios", "Ocio", "Salud", "Otros", "Ingresos"));
        }
    }

    private void guardarCategorias() {
        try {
            Files.createDirectories(archivoCategorias.getParent());
            Files.write(archivoCategorias, categorias, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudieron guardar las categorías: " + e.getMessage());
        }
    }

    private void cargarConfig() {
        if (!Files.exists(archivoConfig)) return;
        Properties p = new Properties();
        try (java.io.BufferedReader r = Files.newBufferedReader(archivoConfig, Charset.forName("UTF-8"))) {
            p.load(r);
            presupuestoSemanal = Double.parseDouble(p.getProperty("presupuesto.semanal", "0"));
        } catch (Exception e) {
            System.err.println("No se pudo leer la configuración: " + e.getMessage());
        }
    }

    private void guardarConfig() {
        Properties p = new Properties();
        p.setProperty("presupuesto.semanal", Double.toString(presupuestoSemanal));
        try {
            Files.createDirectories(archivoConfig.getParent());
            try (java.io.Writer w = Files.newBufferedWriter(archivoConfig, Charset.forName("UTF-8"))) {
                p.store(w, "Mis Gastos");
            }
        } catch (IOException e) {
            System.err.println("No se pudo guardar la configuración: " + e.getMessage());
        }
    }
}
