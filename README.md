# CV RAG Bot 🤖

**Tamamen yerel çalışan** (API key gerektirmeyen) bir Retrieval-Augmented
Generation (RAG) uygulaması. Bir CV/portföy dokümanı üzerinde **kaynağa dayalı**
ve halüsinasyonu azaltılmış soru-cevap yapar.

RAG pipeline'ı (embedding üretimi, pgvector'da benzerlik araması, prompt kurulumu)
bir framework'e devredilmeden **elle** yazılmıştır — Ollama'ya kendi `RestClient`
çağrıları, pgvector'a kendi native SQL'i. Amaç: mekanizmayı gizlemeden göstermek.

## Neden bu proje?
- **RAG** güncel bir konu ve backend + AI kesişiminde "çalışır kanıt" sunar.
- Ollama sayesinde **ücretsiz ve yerel** — inceleyen kişi kendi makinesinde çalıştırabilir.
- `pgvector` (`<=>` cosine operatörü, HNSW index), `RestClient`, `JdbcTemplate`
  gibi prod'da kullanılan araçları **kaputun altını göstererek** kullanır.

## Mimari

```
Kullanıcı sorusu
      │
      ▼
ChatController (REST /api/v1/ask)
      │
      ▼
RagService (RAG orkestrasyonu — elle)
      │
      ├─ 1) OllamaClient.embed(soru)      → 768'lik vektör  (POST /api/embeddings)
      ├─ 2) PgVectorStore.search(vec)     → en yakın CV chunk'ları
      │        SQL: ORDER BY embedding <=> ?::vector LIMIT k
      ├─ 3) chunk'ları prompt'a bağlam olarak diz
      ▼
OllamaClient.chat(system, user)          → yanıt  (POST /api/chat, qwen2.5:7b)
```

İndeksleme (uygulama açılışında `CvIngestionRunner`):
`cv.md` → **başlık-farkında (section-aware) böl** (her Markdown bölümü başlığıyla
birlikte bir chunk) → `OllamaClient.embed` → `PgVectorStore.save`. Başlık-farkında
bölme, bir başlığın (ör. bir iş deneyimi) içeriğinden koparılmasını önler.

## Teknolojiler
| Katman | Araç |
|---|---|
| Framework | Spring Boot 3.3 (web + JDBC) — **Spring AI yok, RAG elle** |
| LLM & Embedding | Ollama (`qwen2.5:7b`, `nomic-embed-text`) — yerel |
| Vektör deposu | PostgreSQL + `pgvector` (`vector(768)`, HNSW, cosine `<=>`) |
| HTTP / Persistence | `RestClient` (Ollama) + `JdbcTemplate` (native SQL) |
| Çalıştırma | Docker Compose, Maven, Java 21 |

## Bileşenler
- `llm/OllamaClient` — Ollama `/api/embeddings` ve `/api/chat`'e HTTP çağrıları.
- `vectorstore/PgVectorStore` — pgvector şeması (tablo + HNSW index), INSERT ve
  cosine benzerlik araması; hepsi görünür SQL.
- `rag/RagService` — embed → search → prompt → chat orkestrasyonu.
- `ingestion/CvIngestionRunner` — CV'yi bölüp embed'leyip indeksler.

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
Tarayıcıda **http://localhost:8081** — basit sohbet arayüzü.

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
- RAG pipeline'ının üç fazı: **indeksleme → retrieval → generation** — elle kurulmuş.
- pgvector mekaniği: `vector(N)` tipi, `<=>` cosine mesafesi, HNSW index.
- Embedding boyutu (`768`) ile vektör kolonu boyutunun **eşleşmesi** zorunluluğu.
- Prompt'un dili tek dile sabitlemesi (qwen'in çok dilli token sızıntısını önleme).
- Yerel LLM ile maliyetsiz, gizliliği koruyan (veri dışarı çıkmaz) mimari.

## Yol haritası / bilinen sınırlamalar
- **Çok dilli embedding (öncelikli):** `nomic-embed-text` İngilizce-ağırlıklıdır;
  Türkçe bir sorgu, ilgili İngilizce chunk'ı düşük sıralayabilir. Bu yüzden şu an
  `top-k` yüksek tutulup 1 sayfalık CV bütün olarak getiriliyor. `bge-m3` gibi çok
  dilli bir embed modeline geçmek (1024 boyut → şema + `dimensions` güncellenir,
  yeniden indeksleme gerekir) düşük `top-k` ile **seçici retrieval'ı** güvenilir kılar.
- **Streaming yanıt:** şu an tek seferde dönüyor; `/api/chat` stream'e çevrilebilir.
- **Kaynak gösterme:** her chunk'ın `section` bilgisi saklanıyor; yanıtta hangi CV
  bölümünden geldiğini döndürmek kolayca eklenebilir.
