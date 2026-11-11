-- =====================================================================
-- CestaCerta — migração para a importação inicial de dados
-- Rodar UMA vez no SQL Editor do Supabase, depois do schema.sql.
-- Pode ser rodado de novo sem erro (é idempotente).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. COLUNA "origem": de onde veio cada registro
--    'vendedor'          -> cadastrado pelo app (padrão, o app não muda nada)
--    'osm'               -> mercado importado do OpenStreetMap
--    'openfoodfacts'     -> produto importado do Open Food Facts
--    'openbeautyfacts'   -> produto importado do Open Beauty Facts
--    'openproductsfacts' -> produto importado do Open Products Facts
--    'demo'              -> preço de demonstração (NÃO é preço real)
-- ---------------------------------------------------------------------
alter table empresas add column if not exists origem text not null default 'vendedor';
alter table produtos add column if not exists origem text not null default 'vendedor';
alter table precos   add column if not exists origem text not null default 'vendedor';

-- Identificador do mercado no OpenStreetMap (ex.: 'node/123456'),
-- para o script não duplicar mercados quando rodar de novo.
alter table empresas add column if not exists osm_id text unique;

-- ---------------------------------------------------------------------
-- 2. USUÁRIO "SISTEMA": dono dos mercados importados
--    Sem senha, então ninguém consegue fazer login com ele.
--    O trigger ao_criar_usuario cria o perfil (tipo vendedor) sozinho.
--    Os campos de token vazios ('') evitam erro no painel de usuários
--    do Supabase, que não aceita NULL nessas colunas.
-- ---------------------------------------------------------------------
insert into auth.users (
  id, instance_id, aud, role, email,
  raw_app_meta_data, raw_user_meta_data,
  confirmation_token, recovery_token, email_change_token_new, email_change,
  created_at, updated_at
) values (
  '00000000-0000-4000-8000-000000000001',
  '00000000-0000-0000-0000-000000000000',
  'authenticated', 'authenticated',
  'sistema@cestacerta.invalid',
  '{"provider": "email", "providers": ["email"]}',
  '{"full_name": "CestaCerta (importação)", "tipo": "vendedor"}',
  '', '', '', '',
  now(), now()
)
on conflict (id) do nothing;

-- Garante o perfil mesmo se o trigger não tiver rodado
insert into public.perfis (id, nome, tipo)
values ('00000000-0000-4000-8000-000000000001', 'CestaCerta (importação)', 'vendedor')
on conflict (id) do nothing;

-- ---------------------------------------------------------------------
-- 3. PARA DESFAZER A IMPORTAÇÃO (só se precisar — descomente e rode)
--    Apaga só o que veio da importação; o que foi cadastrado pelo app fica.
-- ---------------------------------------------------------------------
-- delete from precos   where origem = 'demo';
-- delete from empresas where origem = 'osm';
-- delete from produtos where origem in ('openfoodfacts', 'openbeautyfacts', 'openproductsfacts')
--   and not exists (select 1 from precos p where p.produto_id = produtos.id);
