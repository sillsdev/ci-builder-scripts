#!/usr/bin/perl
# Copyright (c) 2017 SIL International
# This software is licensed under the MIT license (http://opensource.org/licenses/MIT)

# Change the source and binary package names in debian/control by appending the value passed as
# argument, e.g. change-package-name.pl -alpha
use strict;
use warnings;

use Dpkg::Control::Info;
use Dpkg::Deps;

my $append = $ARGV[0];
my $control = Dpkg::Control::Info->new();
my $sourceFields = $control->get_source();

# Append $append to source name
$sourceFields->{'Source'} = $sourceFields->{'Source'} . $append;

# Retrieve all packages that we build from this source package
my %allPackages;
foreach my $package ($control->get_packages()) {
	$allPackages{$package->{'Package'}} = $package;
}

# Check dependencies and update if necessary
$sourceFields->{'Build-Depends'} = ReplaceDependencies($sourceFields->{'Build-Depends'});

foreach my $packageName (keys %allPackages) {
	my $package = $allPackages{$packageName};
	$package->{'Package'} = $package->{'Package'} . $append;
	GetAndReplaceDependencies($package, 'Depends');
	GetAndReplaceDependencies($package, 'Suggests');
	GetAndReplaceDependencies($package, 'Recommends');
	GetAndReplaceDependencies($package, 'Conflicts');
	GetAndReplaceDependencies($package, 'Replaces');
	$allPackages{$packageName} = $package;
}

open(FH, '>', 'debian/control') or die $!;
print FH $sourceFields . "\n";
print FH map { $allPackages{$_} . "\n" } keys %allPackages;
close(FH);

sub ProcessDependency {
	my $dep = $_[0];
	my $type = ref $dep;
	if ($type eq "Dpkg::Deps::Simple") {
		$dep->{package} = $dep->{package} . $append if (exists($allPackages{$dep->{package}}));
	} else {
		deps_iterate($dep, sub {
			ProcessDependency($_[0]);
		});
	}
	return 1;
}

sub ReplaceDependencies {
	return "" unless($_[0]);

	my $input = $_[0] =~ s/\n//rg;
	$input = $input =~ s/, /,/rg;
	my @dependencies = split ',', $input;
	my @newDeps;
	foreach my $depStr (@dependencies) {
		if ($depStr =~ m/\$\{/) {
			# if $depStr is a variable, simply copy it
			push @newDeps, $depStr;
		} else {
			my $dep = deps_parse($depStr);
			ProcessDependency($dep);
			push @newDeps, $dep;
		}
	}
	return join ', ', @newDeps;
}

sub GetAndReplaceDependencies {
	my $package = $_[0];
	my $field = $_[1];
	my $section = $package->{$field};
	return unless($section);

	$package->{$field} = ReplaceDependencies($section);
}
