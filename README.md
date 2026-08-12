# CV RAG Bot 🤖

Spring AI ile geliştirilmiş, **tamamen yerel çalışan** (API key gerektirmeyen)
bir Retrieval-Augmented Generation (RAG) uygulaması. Bir CV/portföy dokümanı
üzerinde, **kaynağa dayalı** ve halüsinasyonu azaltılmış soru-cevap yapar.

## Neden bu proje?
- **RAG** güncel bir konu ve backend + AI kesişiminde "çalışır kanıt" sunar.
- Ollama sayesinde **ücretsiz ve yerel** — inceleyen kişi kendi makinesinde çalıştırabilir.
- `pgvector`, `Spring AI`, `Docker` gibi prod'da kullanılan araçları gösterir.

## Mimari

```
Kullanıcı sorusu
      │
      ▼
ChatController (REST /api/ask)
      │
      ▼
RagService ── QuestionAnswerAdvisor
      │              │
      │              ├─ 1) soruyu embedding'e çevir (Ollama: nomic-embed-text)
      │              ├─ 2) pgvector'da en yakın CV parçalarını bul (topK=8)
      │              └─ 3) parçaları prompt'a bağlam olarak ekle
      ▼
Ollama LLM (qwen2.5:7b) ── "sadece bu bağlama göre cevapla" ──► yanıt
```

Indeksleme (uygulama açılışında `CvIngestionRunner`):
`cv.md` → **başlık-farkında (section-aware) böl** (her Markdown bölümü başlığıyla
birlikte bir chunk) → embed → `pgvector`'a yaz. Bu, bir başlığın (ör. bir iş
deneyimi) içeriğinden koparılmasını önler ve retrieval doğruluğunu artırır.

## Teknolojiler
| Katman | Araç |
|---|---|
| Framework | Spring Boot 3.3, Spring AI 1.0 |
| LLM & Embedding | Ollama (`qwen2.5:7b`, `nomic-embed-text`) — yerel |
| Vektör deposu | PostgreSQL + `pgvector` |
| Çalıştırma | Docker Compose, Maven, Java 21 |

## Çalıştırma

### 0. Gereksinimler
- Java 21, Maven, Docker
- [Ollama](https://ollama.com) kurulu

### 1. Modelleri indir (bir kez)
```bash
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```

### 2. pgvector'ı başlat
```bash
docker compose up -d
```

### 3. Uygulamayı çalıştır
```bash
./mvnw spring-boot:run   # ya da: mvn spring-boot:run
```

### 4. Kullan
Tarayıcıda **http://localhost:8080** — basit sohbet arayüzü.

Veya API ile:
```bash
curl -s http://localhost:8081/api/v1/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"Ege'\''nin Spring deneyimi ne?"}'
```

## Kendi verinle özelleştir
`src/main/resources/docs/cv.md` dosyasını kendi CV'nle değiştir. Uygulama her
açılışta yeniden indeksler (`app.ingestion.reload-on-startup=true`).

## Öğrenilenler / Öne çıkan noktalar
- RAG pipeline'ının üç fazı: **indeksleme → retrieval → generation**.
- Embedding boyutu (`768`) ile vektör deposu boyutunun **eşleşmesi** zorunluluğu.
- `QuestionAnswerAdvisor` ile retrieval'ın prompt'a şeffaf entegrasyonu.
- Yerel LLM ile maliyetsiz, gizliliği koruyan (veri dışarı çıkmaz) mimari.
