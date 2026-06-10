ALTER TABLE produto_imagem ADD COLUMN cor_id BIGINT;

-- backfill: cada imagem vai para a 1ª cor do seu produto
UPDATE produto_imagem pi
SET cor_id = (
  SELECT pc.id FROM produto_cor pc
  WHERE pc.produto_id = pi.produto_id
  ORDER BY pc.id LIMIT 1
);

-- remove imagens cujo produto não tem nenhuma cor
DELETE FROM produto_imagem WHERE cor_id IS NULL;

ALTER TABLE produto_imagem
  ADD CONSTRAINT fk_produto_imagem_cor
  FOREIGN KEY (cor_id) REFERENCES produto_cor (id) ON DELETE CASCADE;

ALTER TABLE produto_imagem ALTER COLUMN cor_id SET NOT NULL;
