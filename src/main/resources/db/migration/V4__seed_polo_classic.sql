-- Insere a Polo MAGNOSSÃO Classic com dados do product.ts
INSERT INTO produto (slug, nome, nome_curto, colecao, preco, descricao, descricao_seo, status)
VALUES (
  'classic',
  'Polo MAGNOSSÃO Classic',
  'Polo Classic',
  'Coleção Classic',
  249.90,
  'A Polo MAGNOSSÃO Classic combina elegância nórdica, acabamento premium e identidade brasileira. Produzida em tecido confortável, com bordado discreto da onça na gola e assinatura MAGNOSSÃO no peito.',
  'Polo premium brasileira em algodão pima, com bordado dourado da onça e assinatura MAGNOSSÃO. Raiz nórdica. Alma brasileira.',
  'PUBLICADO'
);

-- Cores
INSERT INTO produto_cor (produto_id, nome, token, hex)
SELECT id, 'Azul Marinho', 'navy',   '#0B1F3A' FROM produto WHERE slug = 'classic';
INSERT INTO produto_cor (produto_id, nome, token, hex)
SELECT id, 'Preto',        'black',  '#111111' FROM produto WHERE slug = 'classic';
INSERT INTO produto_cor (produto_id, nome, token, hex)
SELECT id, 'Branco',       'white',  '#F5F5F5' FROM produto WHERE slug = 'classic';
INSERT INTO produto_cor (produto_id, nome, token, hex)
SELECT id, 'Verde Floresta','forest','#1E3A2A' FROM produto WHERE slug = 'classic';
INSERT INTO produto_cor (produto_id, nome, token, hex)
SELECT id, 'Bege Areia',   'sand',   '#D8C7AE' FROM produto WHERE slug = 'classic';

-- Tamanhos
INSERT INTO produto_tamanho (produto_id, label, peito, comprimento, ombro)
SELECT id, 'P',  50, 68, 42 FROM produto WHERE slug = 'classic';
INSERT INTO produto_tamanho (produto_id, label, peito, comprimento, ombro)
SELECT id, 'M',  53, 70, 44 FROM produto WHERE slug = 'classic';
INSERT INTO produto_tamanho (produto_id, label, peito, comprimento, ombro)
SELECT id, 'G',  56, 72, 46 FROM produto WHERE slug = 'classic';
INSERT INTO produto_tamanho (produto_id, label, peito, comprimento, ombro)
SELECT id, 'GG', 59, 74, 48 FROM produto WHERE slug = 'classic';

-- SKUs (20 combinações: 5 cores × 4 tamanhos)
INSERT INTO sku (produto_id, cor_id, tamanho_id, codigo)
SELECT p.id, c.id, t.id,
       'classic-' || c.token || '-' || lower(t.label)
FROM produto p
JOIN produto_cor c ON c.produto_id = p.id
JOIN produto_tamanho t ON t.produto_id = p.id
WHERE p.slug = 'classic';

-- Benefícios
INSERT INTO produto_beneficio (produto_id, icone_nome, titulo, corpo, ordem)
SELECT id, 'Sparkles', 'Algodão Premium',   'Fios de algodão pima de fibra longa, toque sedoso e alta durabilidade.', 0 FROM produto WHERE slug = 'classic';
INSERT INTO produto_beneficio (produto_id, icone_nome, titulo, corpo, ordem)
SELECT id, 'Feather',  'Bordado Refinado',   'Onça e wordmark bordados em fio dourado fosco, sem brilho excessivo.', 1 FROM produto WHERE slug = 'classic';
INSERT INTO produto_beneficio (produto_id, icone_nome, titulo, corpo, ordem)
SELECT id, 'Square',   'Modelagem Clássica', 'Corte atemporal, ombro estruturado e comprimento equilibrado.', 2 FROM produto WHERE slug = 'classic';
INSERT INTO produto_beneficio (produto_id, icone_nome, titulo, corpo, ordem)
SELECT id, 'MapPin',   'Feita no Brasil',    'Confeccionada em São Paulo com fornecedores selecionados.', 3 FROM produto WHERE slug = 'classic';
INSERT INTO produto_beneficio (produto_id, icone_nome, titulo, corpo, ordem)
SELECT id, 'Infinity', 'Design Atemporal',   'Uma peça que atravessa tendências, pensada para durar décadas.', 4 FROM produto WHERE slug = 'classic';

-- Reviews
INSERT INTO produto_review (produto_id, citacao, nome, cidade)
SELECT id, 'Camisa elegante, tecido muito bom e acabamento acima do esperado.', 'Rafael C.', 'São Paulo, SP' FROM produto WHERE slug = 'classic';
INSERT INTO produto_review (produto_id, citacao, nome, cidade)
SELECT id, 'Discreta e poderosa. Combina com terno e com jeans. Atemporal de verdade.', 'Lucas F.', 'Curitiba, PR' FROM produto WHERE slug = 'classic';
INSERT INTO produto_review (produto_id, citacao, nome, cidade)
SELECT id, 'O bordado da onça é o detalhe que muda tudo. Vai virar minha polo padrão.', 'Henrique B.', 'Belo Horizonte, MG' FROM produto WHERE slug = 'classic';

-- FAQs
INSERT INTO produto_faq (produto_id, pergunta, resposta, ordem)
SELECT id, 'Qual o tecido da polo?', 'Algodão pima de fibra longa, com fios sedosos e respiráveis. Toque premium, alta durabilidade.', 0 FROM produto WHERE slug = 'classic';
INSERT INTO produto_faq (produto_id, pergunta, resposta, ordem)
SELECT id, 'Como funciona a troca?', 'Você tem 30 dias para trocar tamanho ou cor, sem custo, desde que a peça esteja sem uso, com etiquetas e embalagem.', 1 FROM produto WHERE slug = 'classic';
INSERT INTO produto_faq (produto_id, pergunta, resposta, ordem)
SELECT id, 'Tem frete grátis?', 'Frete grátis acima de R$ 299,00 para todo o Brasil.', 2 FROM produto WHERE slug = 'classic';
INSERT INTO produto_faq (produto_id, pergunta, resposta, ordem)
SELECT id, 'Como escolher o tamanho?', 'Use a tabela de medidas. Na dúvida entre dois tamanhos, escolha o menor.', 3 FROM produto WHERE slug = 'classic';
INSERT INTO produto_faq (produto_id, pergunta, resposta, ordem)
SELECT id, 'Quais formas de pagamento?', 'Pix, cartão de crédito em até 3× sem juros, boleto e carteiras digitais.', 4 FROM produto WHERE slug = 'classic';
INSERT INTO produto_faq (produto_id, pergunta, resposta, ordem)
SELECT id, 'A peça encolhe?', 'A modelagem é pré-encolhida. Lavar a até 30°C, sem secadora, mantém o caimento original.', 5 FROM produto WHERE slug = 'classic';
