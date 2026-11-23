package gastos;

import java.time.LocalDate;

/** Un gasto registrado por el usuario. */
public class Gasto {
    public final long id;
    public final double monto;
    public final String cat;
    public final String nota;
    public final LocalDate fecha;
    public final String tipo;

    public Gasto(long id, double monto, String cat, String nota, LocalDate fecha) {
        this(id, monto, cat, nota, fecha, "Gasto");
    }

    public Gasto(long id, double monto, String cat, String nota, LocalDate fecha, String tipo) {
        this.id = id;
        this.monto = monto;
        this.cat = cat;
        this.nota = nota;
        this.fecha = fecha;
        this.tipo = tipo;
    }
}
