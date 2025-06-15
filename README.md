# 📚 V Education - Android Education Platform

Uma plataforma educacional Android moderna que conecta estudantes e educadores, permitindo compartilhamento de estudos, discussões e colaboração em diferentes disciplinas.

## 🎯 **Visão Geral**

O V Education é uma aplicação Android nativa desenvolvida em Kotlin que oferece uma experiência completa de aprendizagem social. Os usuários podem seguir disciplinas, criar e compartilhar estudos, interagir através de votações e comentários, e gerenciar seu perfil educacional.

## ✨ **Funcionalidades Principais**

### 🔐 **Autenticação e Perfil**
- Sistema completo de registro e login
- Perfil personalizável com foto, bio e informações pessoais
- Gerenciamento de sessão seguro
- Alteração de senha e dados pessoais

### 📖 **Sistema de Disciplinas**
- Exploração de disciplinas por categorias
- Sistema de seguir/deixar de seguir disciplinas
- Visualização de estatísticas das disciplinas
- Busca avançada com histórico

### 📝 **Criação e Gestão de Estudos**
- Criação de estudos com diferentes tipos:
  - Perguntas
  - Pedidos de ajuda
  - Documentação
  - Resumos
  - Outros
- Upload de arquivos (PDF, imagens, documentos)
- Editor rico com contagem de caracteres
- Estudos públicos ou privados
- Edição e exclusão de estudos próprios

### 🗳️ **Sistema de Interação**
- Votação em estudos (upvote/downvote)
- Sistema de comentários
- Salvamento de estudos favoritos
- Ordenação por popularidade, data ou "hot"

### 🔍 **Busca e Descoberta**
- Busca global de disciplinas
- Histórico de pesquisas
- Filtros por categoria e popularidade
- Sugestões personalizadas

## 🏗️ **Arquitetura da Base de Dados**

### **Tecnologia**: Supabase (PostgreSQL + Auth + Storage)

### 📊 **Principais Tabelas**

#### **👤 Profiles**
\`\`\`sql
profiles (
    id UUID PRIMARY KEY,
    name TEXT,
    email TEXT,
    bio TEXT,
    profile_image_url TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
\`\`\`

#### **📚 Subjects**
\`\`\`sql
subjects (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    image_url TEXT,
    category_id UUID,
    followers_count INTEGER DEFAULT 0,
    is_featured BOOLEAN DEFAULT false,
    difficulty_level INTEGER DEFAULT 1,
    estimated_hours INTEGER DEFAULT 0,
    created_at TIMESTAMP
)
\`\`\`

#### **📝 Studies**
\`\`\`sql
studies (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    description TEXT,
    study_type TEXT CHECK (study_type IN ('question', 'need_help', 'documentation', 'summary', 'other')),
    subject_id UUID REFERENCES subjects(id),
    author_id UUID REFERENCES auth.users(id),
    status TEXT DEFAULT 'public' CHECK (status IN ('public', 'private')),
    upvotes_count INTEGER DEFAULT 0,
    downvotes_count INTEGER DEFAULT 0,
    comments_count INTEGER DEFAULT 0,
    views_count INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
\`\`\`

#### **🗳️ Study Votes**
\`\`\`sql
study_votes (
    id UUID PRIMARY KEY,
    study_id UUID REFERENCES studies(id),
    user_id UUID REFERENCES auth.users(id),
    vote_type TEXT CHECK (vote_type IN ('upvote', 'downvote')),
    created_at TIMESTAMP,
    UNIQUE(study_id, user_id)
)
\`\`\`

#### **💬 Study Comments**
\`\`\`sql
study_comments (
    id UUID PRIMARY KEY,
    study_id UUID REFERENCES studies(id),
    author_id UUID REFERENCES auth.users(id),
    content TEXT NOT NULL,
    parent_comment_id UUID REFERENCES study_comments(id),
    upvotes_count INTEGER DEFAULT 0,
    downvotes_count INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
\`\`\`

#### **📎 Study Files**
\`\`\`sql
study_files (
    id UUID PRIMARY KEY,
    study_id UUID REFERENCES studies(id),
    file_name TEXT NOT NULL,
    file_url TEXT NOT NULL,
    file_size BIGINT,
    file_type TEXT,
    uploaded_at TIMESTAMP
)
\`\`\`

#### **⭐ Study Saves**
\`\`\`sql
study_saves (
    id UUID PRIMARY KEY,
    study_id UUID REFERENCES studies(id),
    user_id UUID REFERENCES auth.users(id),
    created_at TIMESTAMP,
    UNIQUE(study_id, user_id)
)
\`\`\`

#### **👥 Subject Follows**
\`\`\`sql
subject_follows (
    id UUID PRIMARY KEY,
    subject_id UUID REFERENCES subjects(id),
    user_id UUID REFERENCES auth.users(id),
    created_at TIMESTAMP,
    UNIQUE(subject_id, user_id)
)
\`\`\`

### 🔒 **Segurança (RLS - Row Level Security)**

Todas as tabelas implementam políticas de segurança:
- **Estudos**: Usuários só podem editar/deletar próprios estudos
- **Votos**: Usuários só podem votar uma vez por estudo
- **Comentários**: Usuários só podem editar próprios comentários
- **Arquivos**: Acesso baseado na visibilidade do estudo

### 🔧 **Funções Principais**

#### **📖 Gestão de Estudos**
- `get_studies_by_subject(subject_id, sort_by)` - Lista estudos por disciplina
- `get_my_studies(sort_by)` - Lista estudos do usuário
- `get_study_for_edit(study_id)` - Busca estudo para edição
- `update_study(...)` - Atualiza estudo existente
- `delete_study(study_id)` - Remove estudo e dados associados

#### **🗳️ Sistema de Votação**
- `vote_study(study_id, vote_type)` - Vota em estudo
- `toggle_study_save(study_id)` - Salva/remove estudo dos favoritos

#### **💬 Sistema de Comentários**
- `get_study_comments(study_id)` - Lista comentários
- `post_comment(study_id, content)` - Adiciona comentário
- `vote_comment(comment_id, vote_type)` - Vota em comentário

#### **📚 Gestão de Disciplinas**
- `get_subjects_with_categories()` - Lista disciplinas com categorias
- `toggle_subject_follow(subject_id)` - Segue/deixa de seguir disciplina

## 🛠️ **Tecnologias Utilizadas**

### **Frontend (Android)**
- **Kotlin** - Linguagem principal
- **Android SDK** - Framework nativo
- **RecyclerView** - Listas dinâmicas
- **Coroutines** - Programação assíncrona
- **Material Design** - Interface moderna

### **Backend**
- **Supabase** - Backend-as-a-Service
- **PostgreSQL** - Base de dados relacional
- **Supabase Auth** - Autenticação
- **Supabase Storage** - Armazenamento de arquivos

### **Arquitetura**
- **MVVM Pattern** - Separação de responsabilidades
- **Repository Pattern** - Abstração de dados
- **Singleton Pattern** - Gestão de sessão

## 🚀 **Configuração e Instalação**

### **Pré-requisitos**
- Android Studio Arctic Fox ou superior
- JDK 11 ou superior
- Conta Supabase
- Dispositivo Android (API 21+) ou emulador

### **1. Configuração do Supabase**

1. Crie um projeto no [Supabase](https://supabase.com)
2. Execute os scripts SQL na seguinte ordem:

\`\`\`bash
# Scripts de configuração da base de dados
scripts/create_studies_tables.sql
scripts/setup_storage_buckets.sql
scripts/fix_study_files_bucket.sql
scripts/create_studies_voting_system.sql
scripts/create_get_studies_function.sql
scripts/create_comments_functions.sql
scripts/create_my_studies_function.sql
scripts/create_delete_study_function_v2.sql
scripts/fix_edit_study_functions.sql
\`\`\`

3. Configure as políticas de Storage:
   - Bucket `profile-images`: Público para leitura
   - Bucket `study-files`: Público para leitura

### **2. Configuração do Android**

1. Clone o repositório
2. Abra o projeto no Android Studio
3. Configure as variáveis no `SupabaseClient.kt`:

```kotlin
object SupabaseClient {
    private const val SUPABASE_URL = "SUA_SUPABASE_URL"
    private const val SUPABASE_ANON_KEY = "SUA_SUPABASE_ANON_KEY"
    // ...
}
