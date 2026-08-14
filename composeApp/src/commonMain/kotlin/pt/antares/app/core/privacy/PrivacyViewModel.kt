package pt.antares.app.core.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.antares.app.core.database.entities.SearchMissEntity
import pt.antares.app.core.database.daos.ProgressPhotoDao
import pt.antares.app.core.util.LocalPhotoStore
import pt.antares.app.feature.fooddata.FoodRepository

data class PrivacyUiState(
    val busy: Boolean = false,

    val error: String? = null,

    val searchMisses: List<SearchMissEntity> = emptyList(),

    val importDone: Int? = null,
)

class PrivacyViewModel(
    private val repository: PrivacyRepository,
    private val exporter: DataExporter,
    private val foods: FoodRepository,
    private val photoDao: ProgressPhotoDao,
    private val photos: LocalPhotoStore,
    private val importer: BackupImporter,
) : ViewModel() {

    private val _state = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(searchMisses = foods.topSearchMisses()) }
        }
    }

    fun clearSearchMisses() {
        viewModelScope.launch {
            foods.clearSearchMisses()
            _state.update { it.copy(searchMisses = emptyList()) }
        }
    }

    fun exportData(onReady: (zipName: String, entries: Map<String, ByteArray>) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            runCatching {
                val entries = LinkedHashMap<String, ByteArray>()
                entries[BackupFiles.DATA] = exporter.exportJson().encodeToByteArray()
                exporter.exportCsvFiles().forEach { (nome, csv) ->
                    entries[nome] = csv.encodeToByteArray()
                }

                photoDao.all().forEach { foto ->
                    photos.readBytes(foto.localPath)?.let { bytes ->
                        entries[BackupFiles.PHOTO_DIR + foto.id + ".jpg"] = bytes
                    }
                }
                entries
            }
                .onSuccess { onReady(exporter.zipName(), it) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(busy = false) }
        }
    }

    fun importBackup(entries: Map<String, ByteArray>, modo: ImportMode) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, importDone = null) }

            val dados = entries[BackupFiles.DATA]
            if (dados == null) {
                _state.update {
                    it.copy(busy = false, error = "o ficheiro não tem os dados de um backup do Antares")
                }
                return@launch
            }

            // As fotos são ficheiros e ficam fora da transação da base — que só desfaz
            // linhas. Escrevem-se primeiro de propósito: uma importação falhada deixa
            // ficheiros soltos, que não se veem, e a ordem inversa deixaria linhas a
            // apontar para imagens que não existem. O `BackupCompleteTest` fixa a ordem.
            val caminhos = LinkedHashMap<String, String>()
            for ((nome, bytes) in entries) {
                val id = BackupFiles.photoIdOf(nome) ?: continue
                photos.writeBytes(id, bytes)?.let { caminhos[id] = it }
            }

            when (val r = importer.import(dados.decodeToString(), modo)) {
                is ImportResult.Done -> {

                    caminhos.forEach { (id, caminho) ->
                        photoDao.all().firstOrNull { it.id == id }?.let { linha ->
                            photoDao.upsert(linha.copy(localPath = caminho))
                        }
                    }
                    _state.update { it.copy(busy = false, importDone = r.total) }
                }
                is ImportResult.NotABackup ->
                    _state.update { it.copy(busy = false, error = r.porque) }
                is ImportResult.Failed ->
                    _state.update { it.copy(busy = false, error = r.message) }
            }
        }
    }

    fun clearImportResult() {
        _state.update { it.copy(importDone = null, error = null) }
    }

    fun deleteEverything() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            when (val outcome = repository.deleteEverything()) {
                is WipeOutcome.Failed ->
                    _state.update { it.copy(busy = false, error = outcome.message) }

                WipeOutcome.Success -> _state.update { it.copy(busy = false) }
            }
        }
    }
}
