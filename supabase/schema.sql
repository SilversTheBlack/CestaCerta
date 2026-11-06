
-- =====================================================================
-- CestaCerta — esquema do banco de dados
-- =====================================================================

create extension if not exists pg_trgm;   -- busca de produtos por nome

-- ---------------------------------------------------------------------
-- 1. PERFIS (complementa auth.users, que o Supabase Auth já cria)
--    Login com e-mail/senha, Google, Apple e Facebook fica no Supabase Auth.
-- ---------------------------------------------------------------------
create type tipo_usuario as enum ('cliente', 'vendedor');

create table perfis (
  id         uuid primary key references auth.users(id) on delete cascade,
  nome       text not null,
  tipo       tipo_usuario not null default 'cliente',
  criado_em  timestamptz not null default now()
);

-- Cria o perfil automaticamente quando alguém se cadastra (qualquer provedor)
create or replace function public.criar_perfil() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.perfis (id, nome, tipo)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', new.email),
    case when new.raw_user_meta_data->>'tipo' = 'vendedor'
         then 'vendedor'::public.tipo_usuario
         else 'cliente'::public.tipo_usuario end
  );
  return new;
end; $$;

create trigger ao_criar_usuario
  after insert on auth.users
  for each row execute function public.criar_perfil();

-- ---------------------------------------------------------------------
-- 2. EMPRESAS (mercados) — cadastradas por vendedores, com localização
-- ---------------------------------------------------------------------
create table empresas (
  id               bigint generated always as identity primary key,
  vendedor_id      uuid not null references perfis(id) on delete cascade,
  nome             text not null,
  cnpj             text unique,
  endereco         text not null,
  latitude         double precision not null,
  longitude        double precision not null,
  google_place_id  text,                       -- opcional, vindo da API do Maps
  criado_em        timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- 3. CATEGORIAS e PRODUTOS — catálogo ÚNICO, compartilhado entre mercados
--    (é isso que permite comparar o "mesmo" produto em lojas diferentes)
-- ---------------------------------------------------------------------
create table categorias (
  id     bigint generated always as identity primary key,
  nome   text not null unique,
  icone  text                                  -- nome do drawable ou URL
);

create table produtos (
  id             bigint generated always as identity primary key,
  categoria_id   bigint not null references categorias(id),
  nome           text not null,
  marca          text,
  medida         text,                         -- ex.: '1 kg', '2 L', '12 un'
  codigo_barras  text unique,                  -- EAN: evita produto duplicado
  imagem_url     text
);

create index produtos_categoria_idx on produtos (categoria_id);
create index produtos_nome_trgm_idx on produtos using gin (nome gin_trgm_ops);

-- ---------------------------------------------------------------------
-- 4. PREÇOS — tabela de ligação produto x empresa (1 preço por par)
-- ---------------------------------------------------------------------
create table precos (
  produto_id         bigint not null references produtos(id) on delete cascade,
  empresa_id         bigint not null references empresas(id) on delete cascade,
  preco              numeric(10,2) not null check (preco > 0),
  preco_promocional  numeric(10,2) check (preco_promocional > 0),
  promocao_ate       date,
  atualizado_em      timestamptz not null default now(),
  primary key (produto_id, empresa_id)
);

create index precos_empresa_idx on precos (empresa_id);

create function public.tocar_atualizado_em() returns trigger
language plpgsql as $$
begin
  new.atualizado_em := now();
  return new;
end; $$;

create trigger precos_atualizado_em
  before update on precos
  for each row execute function public.tocar_atualizado_em();

-- ---------------------------------------------------------------------
-- 5. VIEWS usadas pelas telas
-- ---------------------------------------------------------------------

-- Preço que vale hoje (considera promoção ativa)
create view precos_atuais with (security_invoker = true) as
select
  pr.produto_id,
  pr.empresa_id,
  case when pr.preco_promocional is not null
            and (pr.promocao_ate is null or pr.promocao_ate >= current_date)
       then pr.preco_promocional else pr.preco end            as preco_final,
  (pr.preco_promocional is not null
   and (pr.promocao_ate is null or pr.promocao_ate >= current_date)) as em_promocao,
  pr.atualizado_em
from precos pr;

-- Tela "produto selecionado": mercados do mais barato ao mais caro
create view ranking_produto with (security_invoker = true) as
select
  pa.produto_id,
  e.id as empresa_id, e.nome as empresa_nome, e.endereco,
  e.latitude, e.longitude,
  pa.preco_final, pa.em_promocao, pa.atualizado_em
from precos_atuais pa
join empresas e on e.id = pa.empresa_id;
-- uso: select * from ranking_produto where produto_id = ? order by preco_final;

-- Tela "categoria selecionada": quais mercados são mais baratos na categoria.
-- Índice de preço: para cada produto, divide o preço da loja pelo menor preço
-- encontrado; 100 = sempre o mais barato, 115 = em média 15% mais caro.
-- (Comparar soma/média de preços puros seria injusto, porque cada loja
--  vende um conjunto diferente de produtos.)
create view ranking_categoria with (security_invoker = true) as
with base as (
  select
    p.categoria_id, pa.empresa_id, pa.preco_final,
    min(pa.preco_final) over (partition by pa.produto_id) as menor_preco
  from precos_atuais pa
  join produtos p on p.id = pa.produto_id
)
select
  b.categoria_id,
  e.id as empresa_id, e.nome as empresa_nome, e.endereco,
  e.latitude, e.longitude,
  round(avg(b.preco_final / b.menor_preco) * 100, 1) as indice_preco,
  count(*)                                           as qtd_produtos
from base b
join empresas e on e.id = b.empresa_id
group by b.categoria_id, e.id;
-- uso: select * from ranking_categoria where categoria_id = ? order by indice_preco;

-- ---------------------------------------------------------------------
-- 6. SEGURANÇA (Row Level Security)
--    Cliente: só lê. Vendedor: gerencia apenas as próprias empresas/preços.
-- ---------------------------------------------------------------------
alter table perfis     enable row level security;
alter table empresas   enable row level security;
alter table categorias enable row level security;
alter table produtos   enable row level security;
alter table precos     enable row level security;

create policy "perfil: dono lê e edita" on perfis
  for all to authenticated
  using (id = auth.uid()) with check (id = auth.uid());

create policy "empresas: leitura pública" on empresas
  for select using (true);
create policy "empresas: vendedor gerencia as suas" on empresas
  for all to authenticated
  using (vendedor_id = auth.uid())
  with check (vendedor_id = auth.uid()
              and exists (select 1 from perfis
                          where id = auth.uid() and tipo = 'vendedor'));

create policy "categorias: leitura pública" on categorias
  for select using (true);

create policy "produtos: leitura pública" on produtos
  for select using (true);
create policy "produtos: vendedor cadastra" on produtos
  for insert to authenticated
  with check (exists (select 1 from perfis
                      where id = auth.uid() and tipo = 'vendedor'));

create policy "precos: leitura pública" on precos
  for select using (true);
create policy "precos: vendedor gerencia os da sua empresa" on precos
  for all to authenticated
  using (exists (select 1 from empresas e
                 where e.id = empresa_id and e.vendedor_id = auth.uid()))
  with check (exists (select 1 from empresas e
                      where e.id = empresa_id and e.vendedor_id = auth.uid()));

-- ---------------------------------------------------------------------
-- 7. DADOS INICIAIS
-- ---------------------------------------------------------------------
insert into categorias (nome, icone) values
  ('Hortifruti', 'ic_hortifruti'),
  ('Carnes', 'ic_carnes'),
  ('Laticínios', 'ic_laticinios'),
  ('Padaria', 'ic_padaria'),
  ('Mercearia', 'ic_mercearia'),
  ('Bebidas', 'ic_bebidas'),
  ('Limpeza', 'ic_limpeza'),
  ('Higiene', 'ic_higiene');