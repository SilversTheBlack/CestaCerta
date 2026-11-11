"""
Importação inicial de dados do CestaCerta — cidade de São Paulo.

O que faz:
  1. Mercados  -> busca supermercados de São Paulo no OpenStreetMap (Overpass API)
                  e grava em `empresas` (dono = usuário "sistema", origem = 'osm').
  2. Produtos  -> busca produtos vendidos no Brasil no Open Food Facts
                  (e Open Beauty/Products Facts para higiene e limpeza)
                  e grava em `produtos` pelo código de barras (origem = 'openfoodfacts' etc.).
  3. Preços    -> gera preços de DEMONSTRAÇÃO (origem = 'demo') para cada mercado.
                  Não são preços reais; servem só para o app ter conteúdo.

Pré-requisitos:
  - Ter rodado supabase/schema.sql e supabase/migracao_importacao.sql no Supabase.
  - Arquivo seed/.env com SUPABASE_URL e SUPABASE_SERVICE_ROLE_KEY (veja .env.exemplo).

Uso (dentro da pasta seed/):
  python importar_dados.py --simular    # só busca e mostra o resultado, não grava nada
  python importar_dados.py              # busca e grava no Supabase

Pode rodar mais de uma vez: mercados são atualizados pelo osm_id, produtos já
existentes (mesmo código de barras) não são sobrescritos e preços já existentes
não são alterados — nada cadastrado pelo app é sobrescrito.
"""

import argparse
import math
import os
import random
import sys
import time
from datetime import date, timedelta
from pathlib import Path

import requests

SISTEMA_ID = "00000000-0000-4000-8000-000000000001"
USER_AGENT = "CestaCertaSeed/1.0 (projeto academico)"

OVERPASS_URLS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
]

BASES_PRODUTOS = {
    "openfoodfacts": "https://world.openfoodfacts.org",
    "openbeautyfacts": "https://world.openbeautyfacts.org",
    "openproductsfacts": "https://world.openproductsfacts.org",
}

MARCAS_LIMPEZA = ["ype", "minuano", "limpol", "bombril", "brilhante", "tixan", "qboa", "assim",
                  "uau", "veja", "omo", "comfort", "vanish", "pinho-sol", "sanol", "harpic", "ajax"]
MARCAS_HIGIENE = ["sorriso", "protex", "lux", "seda", "monange", "phebo", "granado", "tododia",
                  "neve", "kolynos", "palmolive", "rexona", "elseve", "natura", "colgate", "dove"]

# Nome da categoria no banco -> faixa de preço de demonstração (R$) e de onde buscar.
# Cada fonte é (base, tipo, valor):
#   "categoria" -> busca pela categoria, só produtos marcados como vendidos no Brasil
#                  (countries_tags = países onde é VENDIDO, não onde é fabricado)
#   "marcas"    -> busca pelas marcas, em qualquer país, e filtra pelo código 789/790
CATEGORIAS = {
    "Hortifruti": {
        "faixa": (3, 15),
        "fontes": [("openfoodfacts", "categoria", "en:fresh-fruits"),
                   ("openfoodfacts", "categoria", "en:fresh-vegetables"),
                   ("openfoodfacts", "categoria", "en:frozen-vegetables")],
    },
    "Carnes": {
        "faixa": (15, 60),
        "fontes": [("openfoodfacts", "categoria", "en:meats"),
                   ("openfoodfacts", "categoria", "en:sausages")],
    },
    "Laticínios": {
        "faixa": (4, 30),
        "fontes": [("openfoodfacts", "categoria", "en:dairies")],
    },
    "Padaria": {
        "faixa": (4, 20),
        "fontes": [("openfoodfacts", "categoria", "en:breads"),
                   ("openfoodfacts", "categoria", "en:cakes")],
    },
    "Mercearia": {
        "faixa": (4, 35),
        "fontes": [("openfoodfacts", "categoria", "en:rices"),
                   ("openfoodfacts", "categoria", "en:pastas"),
                   ("openfoodfacts", "categoria", "en:beans"),
                   ("openfoodfacts", "categoria", "en:coffees"),
                   ("openfoodfacts", "categoria", "en:biscuits")],
    },
    "Bebidas": {
        "faixa": (3, 18),
        "fontes": [("openfoodfacts", "categoria", "en:beverages")],
    },
    # Limpeza e higiene quase não têm produtos marcados como "vendido no Brasil",
    # então a busca é por marcas presentes nos mercados brasileiros, aceitando só
    # códigos de barras do GS1 Brasil (789/790) — ver codigo_brasileiro().
    "Limpeza": {
        "faixa": (3, 25),
        "fontes": [("openproductsfacts", "marcas", MARCAS_LIMPEZA),
                   ("openfoodfacts", "marcas", MARCAS_LIMPEZA)],
    },
    "Higiene": {
        "faixa": (4, 30),
        "fontes": [("openbeautyfacts", "marcas", MARCAS_HIGIENE),
                   ("openfoodfacts", "marcas", MARCAS_HIGIENE)],
    },
}

MAX_PAGINAS = 5  # páginas de 100 resultados por fonte

# Redes grandes: o script dá prioridade a elas na hora de escolher os mercados
REDES_GRANDES = [
    "carrefour", "pão de açúcar", "pao de acucar", "extra", "assaí", "assai",
    "atacadão", "atacadao", "dia", "sonda", "oba", "st marche", "hirota",
    "mambo", "tenda", "sam's club", "makro", "big", "nagumo", "andorinha",
    "mini extra", "minuto pão de açúcar", "carrefour express", "dalben",
]


# ---------------------------------------------------------------------
# Configuração
# ---------------------------------------------------------------------
def carregar_env():
    arquivo = Path(__file__).with_name(".env")
    if arquivo.exists():
        for linha in arquivo.read_text(encoding="utf-8").splitlines():
            linha = linha.strip()
            if linha and not linha.startswith("#") and "=" in linha:
                chave, valor = linha.split("=", 1)
                os.environ.setdefault(chave.strip(), valor.strip().strip('"').strip("'"))


def http_get(url, **kwargs):
    kwargs.setdefault("timeout", 60)
    headers = kwargs.pop("headers", {})
    headers["User-Agent"] = USER_AGENT
    esperas = [10, 30, 60]  # servidores públicos às vezes ficam sobrecarregados (502/503)
    for tentativa in range(len(esperas) + 1):
        try:
            r = requests.get(url, headers=headers, **kwargs)
            if r.status_code == 429:
                print("  (limite de requisições atingido, esperando 60s...)")
                time.sleep(60)
                continue
            r.raise_for_status()
            return r
        except requests.RequestException as e:
            if tentativa == len(esperas):
                raise
            print(f"  (falha: {e}; tentando de novo em {esperas[tentativa]}s)")
            time.sleep(esperas[tentativa])
    raise requests.RequestException(f"Não foi possível acessar {url}")


# ---------------------------------------------------------------------
# 1. Mercados — OpenStreetMap
# ---------------------------------------------------------------------
CONSULTA_OVERPASS = """
[out:json][timeout:180];
area["name"="São Paulo"]["boundary"="administrative"]["admin_level"="8"]->.sp;
nwr["shop"="supermarket"](area.sp);
out center tags;
"""


def buscar_mercados(limite):
    print("Buscando supermercados de São Paulo no OpenStreetMap...")
    dados = None
    for rodada, espera in enumerate([30, 60, 120, None]):
        for url in OVERPASS_URLS:
            try:
                r = requests.post(url, data={"data": CONSULTA_OVERPASS},
                                  headers={"User-Agent": USER_AGENT}, timeout=240)
                r.raise_for_status()
                dados = r.json()
                break
            except (requests.RequestException, ValueError) as e:
                print(f"  ({url} falhou: {str(e)[:120]})")
        if dados is not None or espera is None:
            break
        print(f"  Servidores ocupados; tentando de novo em {espera}s...")
        time.sleep(espera)
    if dados is None:
        raise SystemExit("Nenhum servidor Overpass respondeu. Tente de novo mais tarde.")

    mercados = []
    for el in dados.get("elements", []):
        tags = el.get("tags", {})
        nome = tags.get("name") or tags.get("brand")
        lat = el.get("lat") or el.get("center", {}).get("lat")
        lon = el.get("lon") or el.get("center", {}).get("lon")
        if not nome or lat is None or lon is None:
            continue

        rua = tags.get("addr:street")
        partes = []
        if rua:
            partes.append(f"{rua}, {tags['addr:housenumber']}" if tags.get("addr:housenumber") else rua)
        if tags.get("addr:suburb"):
            partes.append(tags["addr:suburb"])
        partes.append("São Paulo - SP")

        rede = (tags.get("brand") or nome).strip()
        mercados.append({
            "osm_id": f"{el['type']}/{el['id']}",
            "nome": nome.strip(),
            "endereco": " - ".join(partes),
            "latitude": lat,
            "longitude": lon,
            "_rede": rede,
            "_grande": any(r == rede.lower() or rede.lower().startswith(r + " ") for r in REDES_GRANDES),
            "_tem_endereco": bool(rua),
        })

    print(f"  {len(mercados)} supermercados encontrados.")
    return escolher_mercados(mercados, limite)


def escolher_mercados(mercados, limite):
    """Prioriza redes grandes com endereço e alterna entre redes, para não vir só uma."""
    mercados.sort(key=lambda m: (not m["_grande"], not m["_tem_endereco"]))
    por_rede = {}
    for m in mercados:
        por_rede.setdefault(m["_rede"].lower(), []).append(m)
    grupos = sorted(por_rede.values(),
                    key=lambda g: (not g[0]["_grande"], not g[0]["_tem_endereco"], -len(g)))

    escolhidos = []
    while len(escolhidos) < limite and any(grupos):
        for grupo in grupos:
            if grupo and len(escolhidos) < limite:
                escolhidos.append(grupo.pop(0))
        grupos = [g for g in grupos if g]

    for m in escolhidos:
        for chave in ("_rede", "_grande", "_tem_endereco"):
            m.pop(chave)
    print(f"  {len(escolhidos)} escolhidos (limite {limite}).")
    return escolhidos


# ---------------------------------------------------------------------
# 2. Produtos — Open Food / Beauty / Products Facts
# ---------------------------------------------------------------------
def codigo_valido(codigo):
    return codigo.isdigit() and len(codigo) in (8, 12, 13, 14)


def codigo_brasileiro(codigo):
    """EAN-13 com prefixo do GS1 Brasil: produto registrado para o mercado brasileiro."""
    return len(codigo) == 13 and codigo.startswith(("789", "790"))


def buscar_pagina(fonte, tipo, valor, pagina):
    params = {
        "fields": "code,product_name,product_name_pt,brands,quantity,image_front_url",
        "sort_by": "unique_scans_n",
        "page_size": 100,
        "page": pagina,
    }
    if tipo == "categoria":
        params["categories_tags"] = valor
        params["countries_tags"] = "en:brazil"
    else:
        params["brands_tags"] = "|".join(valor)  # "|" = qualquer uma das marcas
    try:
        return http_get(f"{BASES_PRODUTOS[fonte]}/api/v2/search", params=params).json().get("products", [])
    except (requests.RequestException, ValueError) as e:
        print(f"  ({fonte} indisponível agora, pulando essa fonte: {e})")
        return None
    finally:
        time.sleep(6)  # a API de busca permite ~10 requisições por minuto


def buscar_produtos(por_categoria):
    print("Buscando produtos (Open Food Facts e afins)...")
    produtos = {}  # codigo_barras -> produto (evita repetir o mesmo produto em 2 categorias)
    for categoria, info in CATEGORIAS.items():
        quantos = 0
        fontes = info["fontes"]
        for i, (fonte, tipo, valor) in enumerate(fontes):
            # divide o que falta entre as fontes restantes (o que uma não achar, a próxima tenta)
            cota = math.ceil((por_categoria - quantos) / (len(fontes) - i))
            if cota <= 0:
                break
            novos = 0
            for pagina in range(1, MAX_PAGINAS + 1):
                itens = buscar_pagina(fonte, tipo, valor, pagina)
                if not itens:
                    break
                for p in itens:
                    codigo = (p.get("code") or "").strip()
                    nome = (p.get("product_name_pt") or p.get("product_name") or "").strip()
                    if not codigo_valido(codigo) or not nome or codigo in produtos:
                        continue
                    if tipo == "marcas" and not codigo_brasileiro(codigo):
                        continue
                    produtos[codigo] = {
                        "codigo_barras": codigo,
                        "nome": nome[:150],
                        "marca": (p.get("brands") or "").split(",")[0].strip() or None,
                        "medida": (p.get("quantity") or "").strip() or None,
                        "imagem_url": p.get("image_front_url") or None,
                        "origem": fonte,
                        "_categoria": categoria,
                    }
                    novos += 1
                    if novos >= cota:
                        break
                if novos >= cota or len(itens) < 100:
                    break
            quantos += novos
        aviso = "  <- poucos dados nessa fonte" if quantos < 5 else ""
        print(f"  {categoria}: {quantos} produtos{aviso}")
    print(f"  Total: {len(produtos)} produtos.")
    return list(produtos.values())


# ---------------------------------------------------------------------
# 3. Preços de demonstração
# ---------------------------------------------------------------------
def gerar_precos(produtos, mercados, ids_produto, ids_mercado, semente):
    rng = random.Random(semente)
    hoje = date.today()
    base = {p["codigo_barras"]: rng.uniform(*CATEGORIAS[p["_categoria"]]["faixa"]) for p in produtos}

    precos = []
    for m in mercados:
        empresa_id = ids_mercado.get(m["osm_id"])
        if empresa_id is None:
            continue
        fator_mercado = rng.uniform(0.90, 1.15)  # mercado mais barato ou mais caro no geral
        for p in produtos:
            produto_id = ids_produto.get(p["codigo_barras"])
            if produto_id is None or rng.random() > 0.6:  # cada mercado vende ~60% dos produtos
                continue
            preco = round(base[p["codigo_barras"]] * fator_mercado * rng.uniform(0.95, 1.05), 2)
            em_promocao = rng.random() < 0.1
            precos.append({
                "produto_id": produto_id,
                "empresa_id": empresa_id,
                "preco": preco,
                "preco_promocional": round(preco * rng.uniform(0.80, 0.92), 2) if em_promocao else None,
                "promocao_ate": (hoje + timedelta(days=rng.randint(3, 21))).isoformat() if em_promocao else None,
                "origem": "demo",
            })
    return precos


# ---------------------------------------------------------------------
# Supabase (API REST com a chave service_role)
# ---------------------------------------------------------------------
class Supabase:
    def __init__(self, url, chave):
        self.url = url.rstrip("/") + "/rest/v1"
        self.headers = {"apikey": chave, "Content-Type": "application/json"}
        if not chave.startswith("sb_"):  # chave antiga (JWT) também vai no Authorization
            self.headers["Authorization"] = f"Bearer {chave}"

    def _req(self, metodo, tabela, params=None, json=None, prefer=None):
        headers = dict(self.headers)
        if prefer:
            headers["Prefer"] = prefer
        r = requests.request(metodo, f"{self.url}/{tabela}", params=params, json=json,
                             headers=headers, timeout=120)
        if not r.ok:
            raise SystemExit(f"Erro no Supabase ({metodo} {tabela}): {r.status_code} {r.text}")
        return r.json() if r.text else []

    def select(self, tabela, params):
        return self._req("GET", tabela, params=params)

    def upsert(self, tabela, linhas, conflito, ignorar_existentes, retornar=False):
        resolucao = "ignore-duplicates" if ignorar_existentes else "merge-duplicates"
        prefer = f"resolution={resolucao},return={'representation' if retornar else 'minimal'}"
        resultado = []
        for i in range(0, len(linhas), 500):
            resultado += self._req("POST", tabela, params={"on_conflict": conflito},
                                   json=linhas[i:i + 500], prefer=prefer)
        return resultado


def gravar(sb, mercados, produtos, sem_precos, semente):
    if not sb.select("perfis", {"select": "id", "id": f"eq.{SISTEMA_ID}"}):
        raise SystemExit("Usuário 'sistema' não encontrado. Rode supabase/migracao_importacao.sql antes.")

    categorias = {c["nome"]: c["id"] for c in sb.select("categorias", {"select": "id,nome"})}
    faltando = set(CATEGORIAS) - set(categorias)
    if faltando:
        raise SystemExit(f"Categorias ausentes no banco: {', '.join(faltando)}")

    print("Gravando mercados...")
    linhas = [{**m, "vendedor_id": SISTEMA_ID, "origem": "osm"} for m in mercados]
    sb.upsert("empresas", linhas, "osm_id", ignorar_existentes=False)
    ids_mercado = {}
    osm_ids = [m["osm_id"] for m in mercados]
    for i in range(0, len(osm_ids), 100):
        lista = ",".join(f'"{o}"' for o in osm_ids[i:i + 100])
        for e in sb.select("empresas", {"select": "id,osm_id", "osm_id": f"in.({lista})"}):
            ids_mercado[e["osm_id"]] = e["id"]
    print(f"  {len(ids_mercado)} mercados no banco.")

    print("Gravando produtos...")
    linhas = [{k: v for k, v in p.items() if not k.startswith("_")} | {"categoria_id": categorias[p["_categoria"]]}
              for p in produtos]
    sb.upsert("produtos", linhas, "codigo_barras", ignorar_existentes=True)
    ids_produto = {}
    codigos = [p["codigo_barras"] for p in produtos]
    for i in range(0, len(codigos), 100):
        lista = ",".join(codigos[i:i + 100])
        for p in sb.select("produtos", {"select": "id,codigo_barras", "codigo_barras": f"in.({lista})"}):
            ids_produto[p["codigo_barras"]] = p["id"]
    print(f"  {len(ids_produto)} produtos no banco.")

    if sem_precos:
        return
    print("Gerando e gravando preços de demonstração...")
    precos = gerar_precos(produtos, mercados, ids_produto, ids_mercado, semente)
    sb.upsert("precos", precos, "produto_id,empresa_id", ignorar_existentes=True)
    print(f"  {len(precos)} preços enviados (os que já existiam não foram alterados).")


# ---------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(description="Importação inicial de dados do CestaCerta (São Paulo).")
    parser.add_argument("--simular", action="store_true", help="só busca e mostra, não grava no Supabase")
    parser.add_argument("--limite-mercados", type=int, default=40, help="quantos mercados importar (padrão 40)")
    parser.add_argument("--produtos-por-categoria", type=int, default=40, help="máx. de produtos por categoria (padrão 40)")
    parser.add_argument("--sem-precos", action="store_true", help="não gera preços de demonstração")
    parser.add_argument("--semente", type=int, default=42, help="semente dos preços aleatórios (mesma semente = mesmos preços)")
    args = parser.parse_args()

    carregar_env()
    sb = None
    if not args.simular:
        url = os.environ.get("SUPABASE_URL")
        chave = os.environ.get("SUPABASE_SERVICE_ROLE_KEY")
        if not url or not chave:
            raise SystemExit("Preencha SUPABASE_URL e SUPABASE_SERVICE_ROLE_KEY no arquivo seed/.env")
        sb = Supabase(url, chave)

    mercados = buscar_mercados(args.limite_mercados)
    produtos = buscar_produtos(args.produtos_por_categoria)

    if args.simular:
        print("\n--- SIMULAÇÃO (nada foi gravado) ---")
        print("Exemplos de mercados:")
        for m in mercados[:8]:
            print(f"  {m['nome']} | {m['endereco']}")
        print("Exemplos de produtos:")
        for p in produtos[:: max(1, len(produtos) // 8)][:8]:
            print(f"  [{p['_categoria']}] {p['nome']} ({p['marca'] or 'sem marca'}, {p['medida'] or '?'}) {p['codigo_barras']}")
        return

    gravar(sb, mercados, produtos, args.sem_precos, args.semente)
    print("\nPronto!")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()