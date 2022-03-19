# stephenn.com

## development

 	docker run -it --rm -v $PWD:/app -p 4000 ruby:2.6 bash
	cd /app
	gem install bundler:2.0.1 && bundle
	bundle exec jekyll serve --incremental --drafts --host 0.0.0.0

## todo
* html to markdown content for old posts


