#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#


Pod::Spec.new do |s|
  s.name             = 'SDRefeshView'
  s.version          = '0.0.1'
  s.summary          = 'SDRefeshView'
  s.description      = <<-DESC
SDRefeshView
                       DESC
  s.license          = 'MIT' 
  s.homepage         = 'http://tjos.com/seeyon/main.do'
  s.author           = { 'tojoy' => 'xxx@tojoy.com' }

  s.source       = { :svn => "", :tag => s.version.to_s }
  s.ios.deployment_target = '8.0'

  s.source_files = '**/*.{h,m}'


end