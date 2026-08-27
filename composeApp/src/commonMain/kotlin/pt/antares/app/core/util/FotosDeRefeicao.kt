package pt.antares.app.core.util

import pt.antares.app.core.database.daos.FoodLogDao

/**
 * A varredura que decide quanto tempo a fotografia de um prato vive.
 *
 * **A imagem não é o registo.** O registo são os números, e esses ficam para sempre e vão na
 * cópia de segurança. A fotografia é uma ajuda a rever — «foi mesmo isto que comi?» —, e
 * essa pergunta faz-se sobre o mês passado, não sobre 2023.
 *
 * Por isso as fotos dos pratos **não entram na cópia de segurança** e são apagadas ao fim de
 * [DIAS_DE_VIDA] dias. Três refeições fotografadas por dia dão perto de mil imagens por ano;
 * pô-las nas cinco cópias que rodam em `Documentos` enchia o telemóvel de quem nunca pediu
 * isso. As fotos de progresso continuam a ir, porque são poucas e são o próprio dado.
 *
 * Corre no arranque, pela mesma razão que a [pt.antares.app.core.privacy.AutoBackup]: um
 * trabalho agendado que o fabricante mata é pior do que não haver trabalho nenhum.
 */
class FotosDeRefeicao(
    private val dao: FoodLogDao,
    private val fotos: LocalPhotoStore,
    private val hoje: () -> Long = { todayEpochDay() },
) {

    /**
     * Duas passagens, e a ordem importa.
     *
     * Primeiro esquecem-se os caminhos velhos na base, e só depois se apagam os ficheiros
     * que já ninguém refere. Ao contrário, haveria um instante em que a base apontava para
     * um ficheiro que já não existia — e uma app morta a meio deixava-o assim para sempre.
     *
     * Devolve quantos ficheiros foram apagados.
     */
    suspend fun varrer(): Int {
        dao.esquecerFotosAntesDe(hoje() - DIAS_DE_VIDA)

        // O que a base ainda refere, incluindo as linhas apagadas: apagar um registo é
        // desfazível, e a foto tem de sobreviver ao desfazer.
        val referidos = dao.caminhosDeFoto().toSet()

        var apagados = 0
        for (caminho in fotos.listAll()) {
            if (caminho in referidos) continue
            fotos.delete(caminho)
            apagados++
        }
        return apagados
    }

    companion object {

        /**
         * Dois meses. Cobre «o que é que eu comi no mês passado» com folga, e trava o total
         * perto de trinta megabytes — que é o que cabe sem se dar por isso.
         */
        const val DIAS_DE_VIDA = 60L
    }
}
