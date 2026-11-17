package com.aring.movil.data.panel

/** Sugerencias de observación asociadas a cada punto C/R del checklist. */
object ChecklistCommentSuggestions {

    private val estadoChapas = listOf(
        "Chapa dañada o rota.",
        "Chapa trabada.",
        "Chapa sin llave.",
        "Chapa ausente.",
        "Pomeles quebrados.",
        "Pomeles sueltos o desgastados.",
        "Puerta desalineada.",
        "Puerta que no cierra correctamente.",
        "Estructura deformada.",
        "Presencia de óxido o corrosión.",
        "Burletes o sellos deteriorados.",
        "Perforaciones sin sellar.",
        "Pernos de fijación faltantes.",
        "Presencia de humedad o filtraciones."
    )

    private val chasis = listOf(
        "Chasis no aterrizado.",
        "Continuidad de tierra deficiente.",
        "Chasis en mal estado.",
        "Presencia de óxido.",
        "Chasis deformado.",
        "Fijaciones sueltas.",
        "Pernos faltantes.",
        "Presencia de humedad.",
        "Acumulación excesiva de polvo.",
        "Evidencia de sobrecalentamiento."
    )

    private val rotulado = listOf(
        "No rotulado.",
        "Rotulado ilegible.",
        "Rotulado incoherente.",
        "Rotulado desactualizado.",
        "Circuitos sin identificación.",
        "Protecciones sin identificación.",
        "Fases sin identificación.",
        "Ausencia de señalización de riesgo eléctrico.",
        "Diagrama unilineal ausente.",
        "Diagrama unilineal desactualizado."
    )

    private val lucesPiloto = listOf(
        "Luz piloto R dañada.",
        "Luz piloto S dañada.",
        "Luz piloto T dañada.",
        "Luz piloto sin fusible de protección cuando corresponde.",
        "Fusible fundido.",
        "Luz piloto con funcionamiento intermitente.",
        "Conexiones sueltas.",
        "Conductores desconectados.",
        "Portalámparas dañado.",
        "Color de indicador incorrecto.",
        "Evidencia de sobrecalentamiento."
    )

    private val barrasDistribucion = listOf(
        "Barras con temperatura elevada.",
        "Barras con corrosión u oxidación.",
        "Barras con sulfatación.",
        "Barras deformadas.",
        "Conexiones sueltas.",
        "Pernos con apriete deficiente.",
        "Aisladores dañados.",
        "Barras sin protección contra contacto accidental.",
        "Conductores sin férulas cuando corresponde.",
        "Conductores sin aislamiento adecuado.",
        "Conductores con evidencia de derretimiento.",
        "Conductores con sulfatación.",
        "Evidencia de arco eléctrico.",
        "Acumulación excesiva de polvo."
    )

    private val barrasTierra = listOf(
        "Barra de tierra ausente.",
        "Barra desconectada.",
        "Barra con corrosión.",
        "Conexiones sueltas.",
        "Conductores de tierra desconectados.",
        "Conductores de tierra cortados.",
        "Conductores sin terminales adecuados.",
        "Conductores con aislamiento deteriorado.",
        "Conductores con sulfatación.",
        "Continuidad de tierra deficiente.",
        "Conductores de sección inadecuada.",
        "Evidencia de sobrecalentamiento."
    )

    private val barraServicio = listOf(
        "Barra con temperatura elevada.",
        "Barra con corrosión.",
        "Barra con sulfatación.",
        "Conexiones sueltas.",
        "Barra deformada.",
        "Aisladores dañados.",
        "Conductores sin férulas cuando corresponde.",
        "Conductores sin aislamiento adecuado.",
        "Conductores con evidencia de derretimiento.",
        "Conductores con sulfatación.",
        "Conductores de sección inadecuada.",
        "Evidencia de arco eléctrico.",
        "Conexiones improvisadas."
    )

    private val ordenConexionado = listOf(
        "Conductores desordenados.",
        "Conductores sin identificación.",
        "Conductores sin férulas cuando corresponde.",
        "Conductores sin aislamiento adecuado.",
        "Conductores con temperatura elevada.",
        "Conductores con evidencia de derretimiento.",
        "Conductores con sulfatación.",
        "Conexiones sueltas.",
        "Conductores excesivamente tensados.",
        "Conductores sin sujeción.",
        "Canaletas sin tapa.",
        "Canaletas sobreocupadas.",
        "Empalmes improvisados.",
        "Conductores en contacto con bordes cortantes.",
        "Conexionado diferente al plano eléctrico."
    )

    private val conductores = listOf(
        "Conductores sin aislamiento.",
        "Aislamiento deteriorado.",
        "Aislamiento quemado.",
        "Aislamiento derretido.",
        "Conductores con temperatura elevada.",
        "Conductores con sulfatación.",
        "Conductores cortados.",
        "Conductores con hilos dañados.",
        "Conductores subdimensionados.",
        "Conductores sin identificación.",
        "Conexiones sueltas.",
        "Conductores sin terminales adecuados.",
        "Conductores con decoloración térmica.",
        "Conductores expuestos a daños mecánicos.",
        "Resistencia de aislamiento deficiente."
    )

    private val aterrizamiento = listOf(
        "Puerta no aterrizada cuando corresponde.",
        "Contratapa no aterrizada cuando corresponde.",
        "Puerta y contratapa no aterrizadas.",
        "Falta perno de aterrizamiento.",
        "Conductor de tierra cortado.",
        "Conductor de tierra desconectado.",
        "Conductor de tierra deteriorado.",
        "Terminal de tierra suelto.",
        "Terminal con corrosión.",
        "Continuidad eléctrica deficiente.",
        "Conductor de tierra subdimensionado."
    )

    private val acrilicos = listOf(
        "Acrílicos quebrados.",
        "Faltan acrílicos.",
        "Acrílicos sueltos.",
        "Acrílicos deformados.",
        "Acrílicos quemados.",
        "Acrílicos con temperatura elevada.",
        "Acrílicos sin señalización de riesgo eléctrico.",
        "Fijaciones faltantes.",
        "Partes energizadas expuestas.",
        "Acrílicos con perforaciones no autorizadas.",
        "Material protector inadecuado."
    )

    private val terminales = listOf(
        "Faltan terminales.",
        "Terminales sueltos.",
        "Terminales sin aislamiento cuando corresponde.",
        "Terminales subdimensionados.",
        "Terminales sobredimensionados.",
        "Terminales con corrosión.",
        "Terminales con sulfatación.",
        "Terminales quemados.",
        "Terminales con temperatura elevada.",
        "Terminales mal prensados.",
        "Conductores con hilos sueltos.",
        "Conductores mal insertados.",
        "Conexiones con apriete deficiente.",
        "Múltiples conductores en borne no apto.",
        "Evidencia de arco eléctrico."
    )

    private val cajaMoldeada = listOf(
        "Interruptor dañado.",
        "Carcasa quebrada.",
        "Presencia de sulfato.",
        "Conductores sin aislamiento adecuado.",
        "Conductores sin terminales.",
        "Interruptor con temperatura elevada.",
        "Conexiones sueltas.",
        "Interruptor con disparos intempestivos.",
        "Mecanismo de accionamiento dañado.",
        "Interruptor que no permite maniobra.",
        "Protección mal dimensionada.",
        "Ajustes de protección incorrectos.",
        "Evidencia de arco eléctrico.",
        "Presencia de humedad.",
        "Interruptor sin identificación."
    )

    private val equipoMedida = listOf(
        "Equipo con lectura errónea.",
        "Equipo dañado.",
        "Equipo sin lectura.",
        "Pantalla dañada.",
        "Equipo sin alimentación.",
        "Conexiones sueltas.",
        "Bornes dañados.",
        "Configuración incorrecta.",
        "Relación de transformadores de corriente incorrecta.",
        "Fases de medición invertidas.",
        "Pérdida de comunicación.",
        "Alarmas activas.",
        "Equipo con temperatura elevada.",
        "Calibración vencida cuando corresponde."
    )

    private val condensadores = listOf(
        "Condensadores con temperatura elevada.",
        "Condensadores desconectados.",
        "Condensadores quemados.",
        "Condensadores reventados.",
        "Condensadores hinchados.",
        "Condensadores con fugas.",
        "Conexiones sueltas.",
        "Terminales deteriorados.",
        "Pérdida de capacidad.",
        "Fusibles fundidos.",
        "Desequilibrio de corriente.",
        "Contactores quemados.",
        "Etapas que no conectan.",
        "Etapas que no desconectan.",
        "Ventilación insuficiente.",
        "Evidencia de sobrecarga armónica."
    )

    private val contactores = listOf(
        "Contactores quemados.",
        "Contactores desconectados.",
        "Bobina dañada.",
        "Contactor no energiza.",
        "Contactor no desenergiza.",
        "Funcionamiento intermitente.",
        "Contactos desgastados.",
        "Contactos soldados.",
        "Contactos carbonizados.",
        "Ruido anormal.",
        "Vibración excesiva.",
        "Temperatura elevada.",
        "Conexiones sueltas.",
        "Terminales sulfatados.",
        "Mecanismo trabado.",
        "Contactos auxiliares defectuosos."
    )

    private val releFactorPotencia = listOf(
        "Relé sin alimentación.",
        "Pantalla dañada.",
        "Lectura incorrecta.",
        "Configuración incorrecta.",
        "Alarma activa.",
        "Salidas de control defectuosas.",
        "No conecta etapas.",
        "No desconecta etapas.",
        "Factor de potencia fuera del objetivo.",
        "Secuencia de etapas incorrecta.",
        "Relación de transformador de corriente incorrecta.",
        "Señal de corriente ausente.",
        "Conexión de tensión incorrecta.",
        "Bornes deteriorados.",
        "Pérdida de programación.",
        "Funcionamiento intermitente."
    )

    private val termostato = listOf(
        "Falta termostato cuando se requiere.",
        "Termostato desconectado.",
        "Termostato dañado.",
        "Lectura de temperatura incorrecta.",
        "Configuración incorrecta.",
        "No activa ventiladores.",
        "No desactiva ventiladores.",
        "Sensor defectuoso.",
        "Conexiones sueltas.",
        "Contactos defectuosos.",
        "Funcionamiento intermitente.",
        "Ubicación incorrecta."
    )

    private val ventiladores = listOf(
        "Sin ventiladores cuando se requieren.",
        "Ventiladores invertidos.",
        "Ventiladores dañados.",
        "Ventiladores desconectados.",
        "Ventiladores no encienden.",
        "Ventiladores no se detienen.",
        "Ruido excesivo.",
        "Vibración anormal.",
        "Aspas dañadas.",
        "Rejillas obstruidas.",
        "Filtros saturados.",
        "Acumulación excesiva de polvo.",
        "Conexiones sueltas.",
        "Temperatura elevada.",
        "Caudal de aire insuficiente."
    )

    private val relojHorario = listOf(
        "Reloj horario ausente cuando se requiere.",
        "Reloj sin alimentación.",
        "Reloj dañado.",
        "Hora incorrecta.",
        "Programación incorrecta.",
        "Reloj desprogramado.",
        "No activa la salida.",
        "No desactiva la salida.",
        "Contactos defectuosos.",
        "Reserva de marcha agotada.",
        "Conexiones sueltas.",
        "Funcionamiento intermitente.",
        "Selector manual/automático incorrecto."
    )

    private val termografia = listOf(
        "Alta temperatura en barras de distribución.",
        "Alta temperatura en barras de tierra o neutro.",
        "Alta temperatura en terminales.",
        "Alta temperatura en interruptores.",
        "Alta temperatura en contactores.",
        "Alta temperatura en conductores.",
        "Alta temperatura en fusibles.",
        "Alta temperatura en condensadores.",
        "Desequilibrio térmico entre fases.",
        "Punto caliente en conexión.",
        "Punto caliente en empalme.",
        "Temperatura anormal respecto de componentes equivalentes.",
        "Posible conexión deficiente.",
        "Posible sobrecarga.",
        "Temperatura elevada en transformadores de corriente.",
        "Medición no concluyente por condiciones de carga."
    )

    private val porItem = mapOf(
        "tri_c1" to estadoChapas,
        "tri_c2" to chasis,
        "tri_c3" to rotulado,
        "tri_c4" to lucesPiloto,
        "tri_c5" to barrasDistribucion,
        "tri_c6" to barrasTierra,
        "tri_c7" to barraServicio,
        "tri_c8" to ordenConexionado,
        "tri_c9" to conductores,
        "tri_c10" to aterrizamiento,
        "tri_c11" to acrilicos,
        "tri_c12" to terminales,
        "tri_c13" to cajaMoldeada,
        "tri_c14" to equipoMedida,
        "tri_c15" to condensadores,
        "tri_c16" to contactores,
        "tri_c17" to releFactorPotencia,
        "tri_c18" to termostato,
        "tri_c19" to ventiladores,
        "tri_c20" to relojHorario,
        "tri_c21" to termografia,
        "mono_c1" to estadoChapas,
        "mono_c2" to rotulado,
        "mono_c3" to barrasDistribucion,
        "mono_c4" to barrasTierra,
        "mono_c5" to ordenConexionado,
        "mono_c6" to conductores,
        "mono_c7" to acrilicos,
        "mono_c8" to terminales,
        "mono_c9" to termografia
    )

    val observacionesGenerales = listOf(
        "Falta señal de riesgo eléctrico.",
        "Faltan () separadores de fases."
    )

    fun paraItem(itemId: String): List<String> = porItem[itemId].orEmpty()
}
